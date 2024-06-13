package net.lucaciresearch.mqttbridge.implementations.pipewire;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
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
import net.lucaciresearch.mqttbridge.util.PollSpeed;
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

    private final PublishSubject<Boolean> isOpenObservable = PublishSubject.create();

    private final PublishSubject<KeyValue<Map<String, Float>>> notificationObservable = PublishSubject.create();
    private boolean isOpen = false;

    private final List<VariableNode<?, Map<String, Float>>> variableNodes = new ArrayList<>();

    private final List<PipewireConfig.FilterChain> nodeDescriptionWhitelist = List.of(new PipewireConfig.FilterChain("Livingroom Eqializer", "liveq", List.of("eq-sd", "eq-lr", "eq-fc")));

    private final Map<PipewireConfig.FilterChain, Integer> pipewireIdCache = new HashMap<>();

    @Override
    public void initializeConnection() {
        isOpen = true;
        isOpenObservable.onNext(true);
    }

    @Override
    public List<VariableNode<?, Map<String, Float>>> getNodes() {
        return variableNodes;
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public Map<String, Float> readValue(String deviceKey, boolean fastFail) throws ConnectionFailedException, CallFailException {
        PipewireVariableNode variable = variableNodes.stream().filter(n -> n.deviceKey().equals(deviceKey)).map(PipewireVariableNode.class::cast).findAny().orElse(null);
        if (variable == null)
            throw new CallFailException("Variable " + deviceKey + "does not exist");

        PipewireConfig.FilterChain filterChain = nodeDescriptionWhitelist.stream().filter(fc -> fc.subtopic.equals(variable.mqttSubtopic().split("/")[0])).findAny().orElse(null);
        if (filterChain == null)
            throw new CallFailException("Pipewire filter " + deviceKey + " not found in config");

        Integer id = pipewireIdCache.get(filterChain);
        if (id == null)
            throw new CallFailException("Pipewire filter-chain " + filterChain.subtopic() + " not found in id cache");

        try {
            Map<String, Float> params = new HashMap<>();
            String enumparams = executeCommand(List.of("pw-cli", "enum-params", id.toString(), "2"));
            String[] parts = enumparams.split("type Spa:Pod:Object:Param:Props");
            for (String part : parts) {
                if (!part.contains("Prop: key Spa:Pod:Object:Param:Props:params")) {
                    continue;
                }
                Matcher m = propertyScanner.matcher(part);
                while (m.find()) {
                    String filter = m.group(1);
                    String parameter = m.group(2);
                    Float value = Float.parseFloat(m.group(3));
                    if (!filterChain.plugins().contains(filter)) {
                        continue; // TODO: Here we can implement sending notifications to other filters that end up dumped together with this one
                    }
                    params.put(parameter, value);
                }
            }
            return params;
        } catch (IOException e) {
            throw new CallFailException(e.getMessage());
        }

    }

    @Override
    public Map<String, Float> writeValue(String deviceKey, Map<String, Float> deviceValue, boolean fastFail) throws ConnectionFailedException, CallFailException {
        return null;
    }

    @Override
    public Observable<Boolean> isOpenStream() {
        return isOpenObservable;
    }

    @Override
    public Observable<KeyValue<Map<String, Float>>> notifyValue() {
        return notificationObservable;
    }

    @Override
    public void closeConnection() {
        isOpen = false;
        isOpenObservable.onNext(false);
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
            Map<String, Map<String, Float>> mins = new HashMap<>();
            Map<String, Map<String, Float>> maxs = new HashMap<>();
            String[] parts = enumparams.split("type Spa:Pod:Object:Param:Props");
            for (String part : parts) {
                if (!part.contains("Prop: key Spa:Pod:Object:Param:Props:params"))
                    continue;
                Matcher m = propertyScanner.matcher(part);
                while (m.find()) {
                    String filter = m.group(1);
                    if (!filterChain.plugins().contains(filter))
                        continue;
                    String parameter = m.group(2);
                    int position = infodump.indexOf(filter + ":" + parameter);
                    if (position == -1)
                        continue;

                    Matcher minmax = propertyInfoScanner.matcher(infodump);
                    if (!minmax.find(position))
                        continue;
                    Float min = Float.parseFloat(minmax.group(2));
                    Float max = Float.parseFloat(minmax.group(3));
                    mins.computeIfAbsent(filter, f -> new HashMap<>()).put(parameter, min);
                    maxs.computeIfAbsent(filter, f -> new HashMap<>()).put(parameter, max);
                }
            }
            for (String filter : mins.keySet()) {
                variableNodes.add(new PipewireVariableNode(PollSpeed.SLOW, filter, mins.get(filter), maxs.get(filter), filterChain.subtopic()));
            }
            pipewireIdCache.put(filterChain, id);
        }


    }

    private String executeCommand(List<String> pwcliArguments) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(pwcliArguments);
        try {
            Process process = pb.start();
            InputStream stream = process.getInputStream();
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[10240];
            for (int length; (length = stream.read(buffer)) != -1; ) {
                result.write(buffer, 0, length);
            }
            boolean success = process.waitFor(50000000, TimeUnit.MILLISECONDS);
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

@Getter
@Setter
@Accessors(fluent = true, chain = true)
@NoArgsConstructor
@AllArgsConstructor
class Parameter {

    private String filter;

    private String name;

}