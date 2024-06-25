package net.lucaciresearch.mqttbridge.implementations.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

@Slf4j
public class TcpConnectionHolder implements DuplexConnectionHolder {

    private final String host;
    private final int port;

    private final int timeout;

    private Socket socket;

    public TcpConnectionHolder(String host, int port, int timeout) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
    }

    @Override
    public void openConnection() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), timeout);
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (socket == null)
            throw new IOException("Not connected");
        return socket.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (socket == null)
            throw new IOException("Not connected");
        return socket.getOutputStream();
    }

    @Override
    public void closeConnection() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                log.warn("Failed to close connection", e);
            }
        }
    }

    @Override
    public String getDescription() {
        return "TCP " + host + ":" + port;
    }
}
