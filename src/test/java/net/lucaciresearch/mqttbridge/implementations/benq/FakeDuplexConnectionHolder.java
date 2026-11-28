package net.lucaciresearch.mqttbridge.implementations.benq;

import net.lucaciresearch.mqttbridge.implementations.util.DuplexConnectionHolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A fake projector connection built on piped streams.
 * Every flushed command is recorded; if a reply was scripted for that exact command, it is written back.
 */
public class FakeDuplexConnectionHolder implements DuplexConnectionHolder {

    private final Map<String, String> replies = new ConcurrentHashMap<>();
    private final List<String> written = new CopyOnWriteArrayList<>();

    private PipedInputStream bridgeIn;
    private PipedOutputStream deviceOut;
    private OutputStream bridgeOut;

    /** Script the projector's answer (raw bytes, including line endings) to an exact command. */
    public FakeDuplexConnectionHolder reply(String command, String response) {
        replies.put(command, response);
        return this;
    }

    /** Everything the bridge flushed to the projector, one entry per flush. */
    public List<String> written() {
        return written;
    }

    /** Send a line the projector produced on its own. */
    public synchronized void sendFromDevice(String data) throws IOException {
        deviceOut.write(data.getBytes(StandardCharsets.US_ASCII));
        deviceOut.flush();
    }

    @Override
    public synchronized void openConnection() throws IOException {
        bridgeIn = new PipedInputStream(4096);
        deviceOut = new PipedOutputStream(bridgeIn);
        bridgeOut = new OutputStream() {
            private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            @Override
            public void write(int b) {
                buffer.write(b);
            }

            @Override
            public void flush() throws IOException {
                String command = buffer.toString(StandardCharsets.US_ASCII);
                buffer.reset();
                if (command.isEmpty())
                    return;
                written.add(command);
                String response = replies.get(command);
                if (response != null)
                    sendFromDevice(response);
            }
        };
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (bridgeIn == null)
            throw new IOException("Not connected");
        return bridgeIn;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (bridgeOut == null)
            throw new IOException("Not connected");
        return bridgeOut;
    }

    @Override
    public synchronized void closeConnection() {
        try {
            if (deviceOut != null)
                deviceOut.close();
        } catch (IOException e) {
            // ignore
        }
    }

    @Override
    public String getDescription() {
        return "fake";
    }
}
