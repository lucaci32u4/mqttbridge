package net.lucaciresearch.mqttbridge.util;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
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


    public static String executeCommand(List<String> processArguments, int msTimeout) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(processArguments);
        try {
            Process process = pb.start();
            InputStream stream = process.getInputStream();
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[10240];
            for (int length; (length = stream.read(buffer)) != -1; ) {
                result.write(buffer, 0, length);
            }
            boolean success = process.waitFor(msTimeout, TimeUnit.MILLISECONDS);
            if (!success)
                throw new IOException("Subprocess took too long to complete");
            if (process.exitValue() != 0)
                throw new IOException("Subprocess exited with non-zero value " + process.exitValue());
            return result.toString(StandardCharsets.UTF_8);

        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for subprocess");
            throw new IOException("Interrupted");
        }

    }

}
