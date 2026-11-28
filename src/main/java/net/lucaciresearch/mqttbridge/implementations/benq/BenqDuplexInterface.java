package net.lucaciresearch.mqttbridge.implementations.benq;

import com.fazecast.jSerialComm.SerialPortTimeoutException;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import lombok.extern.slf4j.Slf4j;
import net.lucaciresearch.mqttbridge.data.VariableNode;
import net.lucaciresearch.mqttbridge.device.DeviceCallInterface;
import net.lucaciresearch.mqttbridge.exceptions.CallFailException;
import net.lucaciresearch.mqttbridge.exceptions.ConnectionFailedException;
import net.lucaciresearch.mqttbridge.implementations.util.DuplexConnectionHolder;
import net.lucaciresearch.mqttbridge.util.ConnectionManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * RS-232 interface for BenQ projectors (W2700 / HT3550 command table).
 * <p>
 * Commands are {@code \r*key=?#\r} (read) and {@code \r*key=value#\r} (write).
 * The projector echoes the command (lines starting with {@code >}), then answers {@code *KEY=VALUE#},
 * or one of {@code Block item}, {@code Illegal format}, {@code Unsupported item}.
 */
@Slf4j
public class BenqDuplexInterface implements DeviceCallInterface<String> {

    public static final long TIMEOUT_MS = 2000;

    private static final List<String> ERROR_REPLIES = List.of("block item", "illegal format", "unsupported item");

    private final List<VariableNode<?, String>> nodes;
    private volatile boolean isOpenFlag = false;

    private final PublishSubject<KeyValue<String>> notifyValueObservable = PublishSubject.create();
    private final PublishSubject<String> pipeLines = PublishSubject.create();
    private final PublishSubject<Boolean> isOpenObservable = PublishSubject.create();
    private final ConnectionManager connectionManager = new ConnectionManager();
    private final Semaphore mutex = new Semaphore(1);

    private final DuplexConnectionHolder connectionHolder;
    private Thread readerThread;

    public BenqDuplexInterface(DuplexConnectionHolder connectionHolder, List<VariableNode<?, String>> nodes) {
        this.nodes = nodes;
        this.connectionHolder = connectionHolder;
        connectionManager.creator(() -> {
            try {
                connectionHolder.openConnection();
                log.info("Connected to BenQ projector {}", connectionHolder.getDescription());
            } catch (IOException e) {
                log.error("Can't connect to BenQ projector at {}: {}", connectionHolder.getDescription(), e.getMessage());
                return false;
            }
            isOpenFlag = true;
            isOpenObservable.onNext(true);
            readerThread = new Thread(this::readLoop, "benq-reader");
            readerThread.setDaemon(true);
            readerThread.start();
            return true;
        });
        connectionManager.destroyer(() -> {
            isOpenFlag = false;
            isOpenObservable.onNext(false);
            if (readerThread != null)
                readerThread.interrupt();
            connectionHolder.closeConnection();
            readerThread = null;
        });
    }

    private void readLoop() {
        try {
            StringBuilder sb = new StringBuilder();
            InputStream stream = connectionHolder.getInputStream();
            while (!Thread.currentThread().isInterrupted()) {
                int c;
                try {
                    c = stream.read();
                } catch (SerialPortTimeoutException e) {
                    continue;
                }
                if (c < 0)
                    break;
                if (c == '\r' || c == '\n') {
                    handleLine(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append((char) c);
                }
            }
        } catch (IOException e) {
            // connection closed or lost
        }
        if (isOpenFlag) {
            log.error("Lost connection to BenQ projector {}", connectionHolder.getDescription());
            connectionManager.markFailed();
        }
    }

    private void handleLine(String raw) {
        String line = raw.trim();
        if (line.isEmpty() || line.startsWith(">"))
            return;
        log.debug("Got from projector {}", line);
        pipeLines.onNext(line);
        KeyValue<String> kv = parseReply(line);
        if (kv == null)
            return;
        for (VariableNode<?, String> node : nodes) {
            if (node.deviceKey().equalsIgnoreCase(kv.key())) {
                notifyValueObservable.onNext(new KeyValue<>(node.deviceKey(), kv.value()));
            }
        }
    }

    /**
     * Parse a reply line of the form {@code *KEY=VALUE#}.
     * @return the key and value without {@code *} and {@code #}, or null if the line has another form
     */
    static KeyValue<String> parseReply(String line) {
        line = line.trim();
        if (!line.startsWith("*") || !line.endsWith("#"))
            return null;
        int eq = line.indexOf('=');
        if (eq < 0)
            return null;
        String key = line.substring(1, eq).trim();
        String value = line.substring(eq + 1, line.length() - 1).trim();
        if (key.isEmpty())
            return null;
        return new KeyValue<>(key, value);
    }

    /**
     * @return the error text if the line is one of the projector's error replies, otherwise null
     */
    static String parseError(String line) {
        String s = line.trim();
        if (s.startsWith("*"))
            s = s.substring(1);
        if (s.endsWith("#"))
            s = s.substring(0, s.length() - 1);
        s = s.trim();
        return ERROR_REPLIES.contains(s.toLowerCase()) ? s : null;
    }

    static String readCommand(String deviceKey) {
        return "\r*" + deviceKey + "=?#\r";
    }

    static String writeCommand(String deviceKey, String deviceValue) {
        return "\r*" + deviceKey + "=" + deviceValue + "#\r";
    }

    @Override
    public void initializeConnection() {
        connectionManager.start();
    }

    @Override
    public List<VariableNode<?, String>> getNodes() {
        return nodes;
    }

    @Override
    public boolean isOpen() {
        return isOpenFlag;
    }

    @Override
    public String readValue(String deviceKey, boolean fastFail) throws ConnectionFailedException, CallFailException {
        if (!isOpenFlag)
            throw new ConnectionFailedException("Not connected");
        return call(deviceKey, readCommand(deviceKey), "reading", fastFail);
    }

    @Override
    public String writeValue(String deviceKey, String deviceValue, boolean fastFail) throws ConnectionFailedException, CallFailException {
        if (!isOpenFlag)
            throw new ConnectionFailedException("Not connected");
        return call(deviceKey, writeCommand(deviceKey, deviceValue), "writing", fastFail);
    }

    private String call(String deviceKey, String command, String action, boolean fastFail) throws ConnectionFailedException, CallFailException {
        Disposable disposable = null;
        mutex.acquireUninterruptibly();
        try {
            if (!isOpenFlag)
                throw new ConnectionFailedException("Not connected");

            log.debug("Start {} {} {}", action, deviceKey, command.trim());

            String[] result = new String[1];
            String[] error = new String[1];
            Semaphore done = new Semaphore(0);
            disposable = pipeLines.subscribe(l -> {
                if (result[0] != null || error[0] != null)
                    return; // already answered
                String err = parseError(l);
                if (err != null) {
                    error[0] = err;
                    done.release();
                    return;
                }
                KeyValue<String> kv = parseReply(l);
                if (kv != null && kv.key().equalsIgnoreCase(deviceKey)) {
                    result[0] = kv.value();
                    done.release();
                }
            });

            OutputStream out = connectionHolder.getOutputStream();
            out.write(command.getBytes(StandardCharsets.US_ASCII));
            out.flush();

            if (!done.tryAcquire(1, TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                if (fastFail)
                    connectionManager.markFailed();
                throw new CallFailException("Timeout");
            }
            // the reader thread sets the values before release(), so they are visible after tryAcquire()
            if (error[0] != null)
                throw new CallFailException(error[0]);
            log.debug("Successfully {} {} {}", action, deviceKey, command.trim());
            return result[0];
        } catch (InterruptedException e) {
            log.error("Unexpected error: {}", e.toString());
            connectionManager.markFailed();
            throw new CallFailException(e.toString());
        } catch (IOException e) {
            log.error("Lost connection to device: {}", e.getMessage());
            connectionManager.markFailed();
            throw new ConnectionFailedException("Lost connection: " + e.getMessage());
        } finally {
            if (disposable != null)
                disposable.dispose();
            mutex.release();
        }
    }

    @Override
    public Observable<Boolean> isOpenStream() {
        return isOpenObservable;
    }

    @Override
    public Observable<KeyValue<String>> notifyValue() {
        return notifyValueObservable;
    }

    @Override
    public void closeConnection() {
        connectionManager.stop();
    }
}
