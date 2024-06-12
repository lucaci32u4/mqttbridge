package net.lucaciresearch.mqttbridge.implementations.pipewire;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import net.lucaciresearch.mqttbridge.data.VariableNode;
import net.lucaciresearch.mqttbridge.device.DeviceCallInterface;
import net.lucaciresearch.mqttbridge.exceptions.CallFailException;
import net.lucaciresearch.mqttbridge.exceptions.ConnectionFailedException;
import net.lucaciresearch.mqttbridge.util.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class PipewireShellInterface implements DeviceCallInterface<Map<String, Float>> {

    private static final Pattern objectIdScanner = Pattern.compile("object\\.serial\\s*=\\s*\"([0-9]+)\"");

    private static final Pattern propertyScanner = Pattern.compile("String\\s*\"([-_\\w]+):([-_\\w]+)\"\\s*Float\\s*([0-9.,]+)");

    private static final Pattern propertyInfoScanner = Pattern.compile("Float\\s*([0-9.,]+)\\s*Float\\s*([0-9.,]+)\\s*Float\\s*([0-9.,]+)");

    private final List<VariableNode<Map<String, Float>, Map<String, Float>>> variableNodes = new ArrayList<>();

    private final List<PipewireConfig.FilterChain> nodeDescriptionWhitelist = List.of(new PipewireConfig.FilterChain("Livingroom Eqializer", List.of("eq-sd")));



    private final Map<PipewireConfig.FilterChain, Integer> pipewireIdCache = new HashMap<>();

    @Override
    public void initializeConnection() {

    }

    @Override
    public List<VariableNode<?, Map<String, Float>>> getNodes() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public Map<String, Float> readValue(String deviceKey, boolean fastFail) throws ConnectionFailedException, CallFailException {
        return null;
    }

    @Override
    public Map<String, Float> writeValue(String deviceKey, Map<String, Float> deviceValue, boolean fastFail) throws ConnectionFailedException, CallFailException {
        return null;
    }

    @Override
    public Observable<Boolean> isOpenStream() {
        return null;
    }

    @Override
    public Observable<KeyValue<Map<String, Float>>> notifyValue() {
        return null;
    }

    @Override
    public void closeConnection() {

    }


    public void discoverFilters() throws IOException {
        String listing = executeCommand(List.of("pw-cli", "ls", "Node"));
        List<Integer> filters = Arrays.stream(listing.split("id\\s[0-9]+"))
                .filter(section -> section.contains("PipeWire:Interface:Node"))
                .filter(section -> section.contains("\"Audio/Sink\""))
                .filter(section -> nodeDescriptionWhitelist.stream().anyMatch(w -> section.contains("\"" + w.description() + "\"")))
                .map(section -> Util.regexGroup(section, objectIdScanner, 1))
                .filter(Objects::nonNull)
                .map(Integer::parseInt)
                .toList();

        for (Integer id : filters) {
            String sect = Arrays.stream(listing.split("id\\s[0-9]+"))
                    .filter(section -> section.contains("PipeWire:Interface:Node"))
                    .filter(section -> section.contains("\"Audio/Sink\""))
                    .filter(section -> Objects.equals(Util.regexGroup(section, objectIdScanner, 1), id.toString()))
                    .findAny().orElse(null);
            if (sect == null)
                continue;

            PipewireConfig.FilterChain filterChain = nodeDescriptionWhitelist.stream()
                    .filter(w -> sect.contains("\"" + w.description() + "\""))
                    .findAny().orElse(null);
            if (filterChain == null)
                continue;

            String enumparams = executeCommand(List.of("pw-cli", "enum-params", id.toString(), "2"));
            String infodump = executeCommand(List.of("pw-cli", "enum-params", id.toString(), "1"));
            String[] parts = enumparams.split("type Spa:Pod:Object:Param:Props");
            for (String part : parts) {
                if (!part.contains("Prop: key Spa:Pod:Object:Param:Props:params"))
                    continue;
                Matcher m = propertyScanner.matcher(part);
                while (m.find()) {
                    String filter = m.group(1);
                    String parameter = m.group(2);
                    int position = infodump.indexOf(filter + ":" + parameter);
                    if (position == -1)
                        continue;

                    Matcher minmax = propertyInfoScanner.matcher(infodump);
                    if (!minmax.find(position))
                        continue;
                    Float min = Float.parseFloat(minmax.group(2));
                    Float max = Float.parseFloat(minmax.group(3));

                }
            }

            pipewireIdCache.put(filterChain, id);


        }


    }

    private String executeCommand(List<String> pwcliArguments) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(pwcliArguments);
        try {
            Process process = pb.start();
            boolean success = process.waitFor(5000, TimeUnit.MILLISECONDS);
            if (!success)
                throw new IOException("Subprocess took too long to complete");
            if (process.exitValue() != 0)
                throw new IOException("Subprocess exited with non-zero value " + process.exitValue());

            InputStream stream = process.getInputStream();
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int length; (length = stream.read(buffer)) != -1; ) {
                result.write(buffer, 0, length);
            }
            return result.toString(StandardCharsets.UTF_8);

        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for subprocess");
            throw new IOException("Interrupted");
        }

    }




}

@Getter
@Setter
@Accessors(fluent = true, chain = true)
@NoArgsConstructor
@AllArgsConstructor
class Parameter {

    private String filter;

    private String name;

}