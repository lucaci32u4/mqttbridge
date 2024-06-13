package net.lucaciresearch.mqttbridge.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    public static Throwable unwrap(Throwable t) {
        while (t != null && t.getCause() != null && t.getClass() == RuntimeException.class) {
            t = t.getCause();
        }
        return t;
    }

    public static String regexGroup(String input, Pattern regex, int group) {
        try {
            Matcher m = regex.matcher(input);
            m.find();
            return m.group(group);
        } catch (Exception e) {
            return null;
        }
    }

}
