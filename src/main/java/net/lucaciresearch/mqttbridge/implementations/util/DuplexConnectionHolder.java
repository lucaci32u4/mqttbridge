package net.lucaciresearch.mqttbridge.implementations.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface DuplexConnectionHolder {

    void openConnection() throws IOException;

    InputStream getInputStream() throws IOException;

    OutputStream getOutputStream() throws IOException;

    void closeConnection();

    String getDescription();


}
