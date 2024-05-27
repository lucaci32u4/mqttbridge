package net.lucaciresearch.mqttbridge.implementations.marantzsr6010;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import lombok.extern.slf4j.Slf4j;
import net.lucaciresearch.mqttbridge.data.VariableNode;
import net.lucaciresearch.mqttbridge.device.DeviceCallInterface;
import net.lucaciresearch.mqttbridge.exceptions.CallFailException;
import net.lucaciresearch.mqttbridge.exceptions.ConnectionFailedException;
import net.lucaciresearch.mqttbridge.implementations.marantz.MarantzVariableNode;
import net.lucaciresearch.mqttbridge.util.ConnectionManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class MarantzTelnetInterface implements DeviceCallInterface<String> {

    private final List<VariableNode<?, String>> nodes;
    private volatile boolean isOpenFlag = false;

    private final PublishSubject<KeyValue<String>> notiofyValueObservable = PublishSubject.create();
    private final PublishSubject<String> pipeLines = PublishSubject.create();
    private final PublishSubject<Boolean> isOpenObservable = PublishSubject.create();
    private final ConnectionManager connectionManager = new ConnectionManager();
    private final Semaphore mutex = new Semaphore(1);

    private volatile String responseResult = null;

    private Socket client;
    private Thread readerThread;

    public MarantzTelnetInterface(MarantzTelnetConfig deviceConfig, List<VariableNode<?, String>> nodes) {
        this.nodes = nodes;
        log.info("Telnet interface for SR6010 is not a real telnet protocol -- it's just a raw TCP socket");
        connectionManager.creator(() -> {
            client = new Socket();
            try {
                client.connect(new InetSocketAddress(deviceConfig.host(), 23), 3000);
                client.setTcpNoDelay(true);
                client.setKeepAlive(true);
                log.info("Connected to Marantz {}", deviceConfig.host());
            } catch (IOException e) {
                log.error("Can't connect to TCP server at {}: {}", deviceConfig.host(), e.getMessage());
                return false;
            }
            isOpenFlag = true;
            isOpenObservable.onNext(true);
            readerThread = new Thread(() -> {
                try {
                    StringBuilder sb = new StringBuilder();
                    InputStream stream = client.getInputStream();
                    while (true) {
                        char c = (char) stream.read();
                        if (c == '\r') {
                            String rsp = sb.toString();
                            log.debug("Got from avr {}", rsp);
                            pipeLines.onNext(rsp);
                            for (VariableNode<?, String> node : nodes) {
                                if (rsp.startsWith(node.deviceKey())) {
                                    notiofyValueObservable.onNext(new KeyValue<>(node.deviceKey(), rsp.substring(node.deviceKey().length()).trim()));
                                }
                            }
                            sb.delete(0, sb.length());
                        } else {
                            sb.append(c);
                        }

                    }
                } catch (IOException e) {
                    return;
                }
            });
            readerThread.start();
            return true;
        });
        connectionManager.destroyer(() -> {
            isOpenFlag = false;
            isOpenObservable.onNext(false);
            try {
                readerThread.interrupt();
                client.close();
            } catch (IOException e) {
                log.warn("Failed to disconnect TCP client: {}", e.getMessage());
            }
            readerThread = null;
            client = null;
        });
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
        MarantzVariableNode<?> mdn = (MarantzVariableNode<?>) nodes.stream().filter(n -> n.deviceKey().equals(deviceKey)).findAny().orElse(null);
        if (mdn == null)
            return null;
        String otherGetterKey = mdn.getterOtherKey();
        return readwriteValue(deviceKey, (otherGetterKey != null ? otherGetterKey : deviceKey) + (mdn.getterExtraSpace() ? " " : "") + "?" + "\r", "reading", fastFail, mdn.getterEndOfResponseString());
    }

    @Override
    public String writeValue(String deviceKey, String deviceValue, boolean fastFail) throws ConnectionFailedException, CallFailException {
        if (!isOpenFlag)
            throw new ConnectionFailedException("Not connected");
        MarantzVariableNode<?> mdn = (MarantzVariableNode<?>) nodes.stream().filter(n -> n.deviceKey().equals(deviceKey)).findAny().orElse(null);
        if (mdn == null)
            return null;
        return readwriteValue(deviceKey, deviceKey + (mdn.setterExtraSpace() ? " " : "") + deviceValue + "\r", "writing", fastFail, null);
    }


    public String readwriteValue(String deviceKey, String command, String action, boolean fastFail, String endOfResponseOptional) throws ConnectionFailedException, CallFailException {
        String answer = null;
        Disposable disposable = null;
        try {
            mutex.acquireUninterruptibly();
            if (!isOpenFlag)
                throw new ConnectionFailedException("Not connected");

            log.debug("Start {} {} {}", action, deviceKey, command);

            responseResult = null;
            Semaphore done = new Semaphore(0, false);
            disposable = pipeLines.subscribe(l -> {
                if (l.startsWith(deviceKey)) {
                    responseResult = l.substring(deviceKey.length()).trim();
                    if (endOfResponseOptional == null) done.release();
                }
                if (endOfResponseOptional != null && l.startsWith(endOfResponseOptional)) {
                    done.release();
                }
            });

            client.getOutputStream().write(command.getBytes(StandardCharsets.UTF_8));
            client.getOutputStream().flush();

            if (!done.tryAcquire(1, 2000, TimeUnit.MILLISECONDS)) {
                throw new TimeoutException("Timeout");
            }

            log.debug("Successfully {} {} {}", action, deviceKey, command);

            answer = responseResult;
            disposable.dispose();

            if (answer == null) {
                throw new CallFailException("Got end of response marker without requested variable");
            }

        } catch (InterruptedException e) {
            log.error("Unexpected error: {}", e.toString());
            connectionManager.markFailed();
            throw new CallFailException(e.toString());
        } catch (TimeoutException e) {
            if (fastFail)
                connectionManager.markFailed();
            throw new CallFailException("Timeout");
        } catch (IOException e) {
            log.error("Lost connection to device: {}", e.getMessage());
            connectionManager.markFailed();
        } finally {
            mutex.release();
            if (disposable != null) {
                disposable.dispose();
            }
        }
        return answer;
    }

    @Override
    public Observable<Boolean> isOpenStream() {
        return isOpenObservable;
    }

    @Override
    public Observable<KeyValue<String>> notifyValue() {
        return notiofyValueObservable;
    }

    @Override
    public void closeConnection() {
        connectionManager.stop();
    }
}