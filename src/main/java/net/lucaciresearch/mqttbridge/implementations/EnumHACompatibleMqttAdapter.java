package net.lucaciresearch.mqttbridge.implementations;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.lucaciresearch.mqttbridge.data.AbstractHACompatibleMqttAdapter;
import net.lucaciresearch.mqttbridge.exceptions.InvalidMqttInput;
import net.lucaciresearch.mqttbridge.mqtt.HAClass;

import javax.naming.OperationNotSupportedException;
import java.util.Map;

public class EnumHACompatibleMqttAdapter extends AbstractHACompatibleMqttAdapter<String> {

    private final Map<String, String> options;

    public EnumHACompatibleMqttAdapter(String hANiceName, HAClass hAClass, Map<String, String> options) {
        super(hANiceName, hAClass);
        this.options = options;
    }

    @Override
    public String parseMqtt(String mqttValue) throws InvalidMqttInput {
        return options.entrySet().stream()
                .filter(kp -> kp.getValue().equals(mqttValue))
                .findAny()
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new InvalidMqttInput(mqttValue + " is not a valid option"));
    }

    @Override
    public String toMqtt(String value) {
        return options.get(value);
    }

    @Override
    public void addMqttDiscovery(ObjectNode json) throws OperationNotSupportedException {
        ArrayNode opt = json.putArray("options");
        options.values().forEach(opt::add);
    }
}
