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
import net.lucaciresearch.mqttbridge.implementations.util.DuplexConnectionHolder;
import net.lucaciresearch.mqttbridge.util.ConnectionManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class MarantzDuplexInterface implements DeviceCallInterface<String> {

    private final List<VariableNode<?, String>> nodes;
    private volatile boolean isOpenFlag = false;

    private final PublishSubject<KeyValue<String>> notiofyValueObservable = PublishSubject.create();
    private final PublishSubject<String> pipeLines = PublishSubject.create();
    private final PublishSubject<Boolean> isOpenObservable = PublishSubject.create();
    private final ConnectionManager connectionManager = new ConnectionManager();
    private final Semaphore mutex = new Semaphore(1);

    private volatile String responseResult = null;

    private final DuplexConnectionHolder connectionHolder;
    private Thread readerThread;

    public MarantzDuplexInterface(DuplexConnectionHolder connectionHolder, List<VariableNode<?, String>> nodes) {
        this.nodes = nodes;
        this.connectionHolder = connectionHolder;
        log.info("Marantz Telnet interface is not a real telnet protocol -- it's just a raw TCP socket");
        connectionManager.creator(() -> {
            try {
                connectionHolder.openConnection();
                log.info("Connected to Marantz {}", connectionHolder.getDescription());
            } catch (IOException e) {
                log.error("Can't connect to Marantz at {}: {}", connectionHolder.getDescription(), e.getMessage());
                return false;
            }
            isOpenFlag = true;
            isOpenObservable.onNext(true);
            readerThread = new Thread(() -> {
                try {
                    StringBuilder sb = new StringBuilder();
                    InputStream stream = connectionHolder.getInputStream();
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
                    // nothing
                }
            });
            readerThread.start();
            return true;
        });
        connectionManager.destroyer(() -> {
            isOpenFlag = false;
            isOpenObservable.onNext(false);
            readerThread.interrupt();
            connectionHolder.closeConnection();
            readerThread = null;
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

            connectionHolder.getOutputStream().write(command.getBytes(StandardCharsets.UTF_8));
            connectionHolder.getOutputStream().flush();

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