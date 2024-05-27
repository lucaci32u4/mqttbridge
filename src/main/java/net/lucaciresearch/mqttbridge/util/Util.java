package net.lucaciresearch.mqttbridge.util;

public class Util {

    public static Throwable unwrap(Throwable t) {
        while (t != null && t.getCause() != null && t.getClass() == RuntimeException.class) {
            t = t.getCause();
        }
        return t;
    }

}
