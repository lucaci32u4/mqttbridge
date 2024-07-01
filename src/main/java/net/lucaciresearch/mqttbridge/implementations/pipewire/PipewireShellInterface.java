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

    private static final Pattern propertyScanner = Pattern.compile("String\\s*\"([-_\\w]+):([-_\\w]+)\"\\s*Float\\s*(-?[0-9.,]+)");

    private static final Pattern propertyInfoScanner = Pattern.compile("Float\\s*(-?[0-9.,]+)\\s*Float\\s*(-?[0-9.,]+)\\s*Float\\s*(-?[0-9.,]+)");

    private final PublishSubject<Boolean> isOpenObservable = PublishSubject.create();

    private final PublishSubject<KeyValue<Map<String, Float>>> notificationObservable = PublishSubject.create();
    private boolean isOpen = false;

    private final List<VariableNode<?, Map<String, Float>>> variableNodes = new ArrayList<>();
    private final Map<PipewireConfig.FilterChain, Integer> pipewireIdCache = new HashMap<>();

    private final List<PipewireConfig.FilterChain> nodeDescriptionWhitelist;

    public PipewireShellInterface(List<PipewireConfig.FilterChain> nodeDescriptionWhitelist) {
        this.nodeDescriptionWhitelist = nodeDescriptionWhitelist;
    }

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
            String enumparams = Util.executeCommand(List.of("pw-cli", "enum-params", id.toString(), "2"), 5000);
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
        PipewireVariableNode variable = variableNodes.stream().filter(n -> n.deviceKey().equals(deviceKey)).map(PipewireVariableNode.class::cast).findAny().orElse(null);
        if (variable == null)
            throw new CallFailException("Variable " + deviceKey + "does not exist");

        PipewireConfig.FilterChain filterChain = nodeDescriptionWhitelist.stream().filter(fc -> fc.subtopic.equals(variable.mqttSubtopic().split("/")[0])).findAny().orElse(null);
        if (filterChain == null)
            throw new CallFailException("Pipewire filter " + deviceKey + " not found in config");

        Integer id = pipewireIdCache.get(filterChain);
        if (id == null)
            throw new CallFailException("Pipewire filter-chain " + filterChain.subtopic() + " not found in id cache");

        String filter = variable.mqttSubtopic().split("/")[1];
        StringBuilder sb = new StringBuilder();
        sb.append("{ params = [ ");
        for (String key : deviceValue.keySet()) {
            String flt = deviceValue.get(key).toString();
            if (!flt.contains("."))
                flt = flt + ".0";
            sb.append("\"").append(filter).append("\"").append(" ").append(flt).append(" ");
        }
        sb.append("] }");

        try {
            String response = Util.executeCommand(List.of("pw-cli", "set-param", id.toString(), "Props", sb.toString()), 5000);
        } catch (IOException e) {
            throw new CallFailException(e.getMessage());
        }

        return deviceValue;
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

        String listing = Util.executeCommand(List.of("pw-cli", "ls", "Node"), 5000);
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

            String enumparams = Util.executeCommand(List.of("pw-cli", "enum-params", id.toString(), "2"), 5000);
            String infodump = Util.executeCommand(List.of("pw-cli", "enum-params", id.toString(), "1"), 5000);
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