package net.lucaciresearch.mqttbridge.implementations.homeassistant;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.lucaciresearch.mqttbridge.data.AbstractHACompatibleMqttAdapter;
import net.lucaciresearch.mqttbridge.exceptions.InvalidMqttInput;
import net.lucaciresearch.mqttbridge.mqtt.HAClass;

import javax.naming.OperationNotSupportedException;

/**
 * Read-only Home Assistant sensor. Values coming from MQTT are always rejected.
 */
public class SensorHAMqttAdapter<Ty> extends AbstractHACompatibleMqttAdapter<Ty> {

    private final String unitOfMeasurement;

    public SensorHAMqttAdapter(String hANiceName) {
        this(hANiceName, null);
    }

    public SensorHAMqttAdapter(String hANiceName, String unitOfMeasurement) {
        super(hANiceName, HAClass.SENSOR);
        this.unitOfMeasurement = unitOfMeasurement;
    }

    @Override
    public Ty parseMqtt(String mqttValue) throws InvalidMqttInput {
        throw new InvalidMqttInput("read-only");
    }

    @Override
    public String toMqtt(Ty value) {
        return String.valueOf(value);
    }

    @Override
    public void addMqttDiscovery(ObjectNode root) throws OperationNotSupportedException {
        // sensors are read-only in Home Assistant
        root.remove("command_topic");
        if (unitOfMeasurement != null)
            root.put("unit_of_measurement", unitOfMeasurement);
    }
}
