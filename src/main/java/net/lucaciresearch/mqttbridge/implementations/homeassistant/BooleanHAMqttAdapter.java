package net.lucaciresearch.mqttbridge.implementations.homeassistant;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.lucaciresearch.mqttbridge.data.AbstractHACompatibleMqttAdapter;
import net.lucaciresearch.mqttbridge.exceptions.InvalidMqttInput;
import net.lucaciresearch.mqttbridge.mqtt.HAClass;

import javax.naming.OperationNotSupportedException;

public class BooleanHAMqttAdapter extends AbstractHACompatibleMqttAdapter<Boolean> {


    public BooleanHAMqttAdapter(String hANiceName, HAClass hAClass) {
        super(hANiceName, hAClass);
    }

    @Override
    public Boolean parseMqtt(String mqttValue) throws InvalidMqttInput {
        if (mqttValue.trim().equalsIgnoreCase("on")) {
            return true;
        }
        if (mqttValue.trim().equalsIgnoreCase("off")) {
            return false;
        }
        throw new InvalidMqttInput(mqttValue + " is not a boolean (on/off) value");
    }

    @Override
    public String toMqtt(Boolean value) {
        return value ? "ON" : "OFF";
    }

    @Override
    public void addMqttDiscovery(ObjectNode root) throws OperationNotSupportedException {
        root.put("payload_on", "ON");
        root.put("payload_off", "OFF");
    }
}
