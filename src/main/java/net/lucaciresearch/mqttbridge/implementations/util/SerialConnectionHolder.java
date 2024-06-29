package net.lucaciresearch.mqttbridge.implementations.util;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


@Slf4j
public class SerialConnectionHolder implements DuplexConnectionHolder {

    private final String port;
    private final int baud;

    private SerialPort socket;



    public SerialConnectionHolder(String port, int baud) {
        this.port = port;
        this.baud = baud;
    }



    @Override
    public void openConnection() throws IOException {
        socket = SerialPort.getCommPort(port);
        socket.setBaudRate(baud);
        socket.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 3, 0);
        socket.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 3, 0);
        socket.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 3, 0);
        socket.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 3, 0);
        boolean success = socket.openPort(10, 1, 1);
        if (!success) {
            throw new IOException("Failed to open serial port at " + port + ". Check it exists, permissions and file type");
        }


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
            socket.closePort();
        }
    }

    @Override
    public String getDescription() {
        return "Serial " + port + "@" + baud;
    }
}
