package net.lucaciresearch.mqttbridge.implementations.homeassistant;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.lucaciresearch.mqttbridge.data.AbstractHACompatibleMqttAdapter;
import net.lucaciresearch.mqttbridge.exceptions.InvalidMqttInput;
import net.lucaciresearch.mqttbridge.mqtt.HAClass;

import javax.naming.OperationNotSupportedException;

public class IntervalNumberDoubleSteppingHAMqttAdapter extends AbstractHACompatibleMqttAdapter<Double> {

    private final double max;

    private final double min;

    private final double step;
    private final String unitOfMeasurement;

    public IntervalNumberDoubleSteppingHAMqttAdapter(String hANiceName, HAClass hAClass, double max, double min, double step, String unitOfMeasurement) {
        super(hANiceName, hAClass);
        this.max = max;
        this.min = min;
        this.step = step;
        this.unitOfMeasurement = unitOfMeasurement;
    }

    @Override
    public Double parseMqtt(String mqttValue) throws InvalidMqttInput {
        double dbl = Double.parseDouble(mqttValue);
        dbl = dbl + (dbl % step);
        if (max < dbl) dbl = max;
        if (min > dbl) dbl = min;
        return dbl;
    }

    @Override
    public String toMqtt(Double value) {
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
