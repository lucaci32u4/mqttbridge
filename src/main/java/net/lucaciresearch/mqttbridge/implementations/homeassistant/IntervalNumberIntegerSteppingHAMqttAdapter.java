package net.lucaciresearch.mqttbridge.implementations.homeassistant;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.lucaciresearch.mqttbridge.data.AbstractHACompatibleMqttAdapter;
import net.lucaciresearch.mqttbridge.exceptions.InvalidMqttInput;
import net.lucaciresearch.mqttbridge.mqtt.HAClass;

import javax.naming.OperationNotSupportedException;

public class IntervalNumberIntegerSteppingHAMqttAdapter extends AbstractHACompatibleMqttAdapter<Integer> {

    private final int max;

    private final int min;

    private final int step;
    private final String unitOfMeasurement;

    public IntervalNumberIntegerSteppingHAMqttAdapter(String hANiceName, HAClass hAClass, int max, int min, int step, String unitOfMeasurement) {
        super(hANiceName, hAClass);
        this.max = max;
        this.min = min;
        this.step = step;
        this.unitOfMeasurement = unitOfMeasurement;
    }

    @Override
    public Integer parseMqtt(String mqttValue) throws InvalidMqttInput {
        int dbl = Integer.parseInt(mqttValue);
        dbl = dbl + (dbl % step);
        if (max < dbl) dbl = max;
        if (min > dbl) dbl = min;
        return dbl;
    }

    @Override
    public String toMqtt(Integer value) {
        return value.toString();
    }

    @Override
    public void addMqttDiscovery(ObjectNode root) throws OperationNotSupportedException {
        root.put("min", min);
        root.put("max", max);
        root.put("step", step);
        if (unitOfMeasurement != null)
            root.put("unit_of_measurement", unitOfMeasurement);
    }
}
