package net.lucaciresearch.mqttbridge.implementations.benq;

import net.lucaciresearch.mqttbridge.data.DeviceAdapter;
import net.lucaciresearch.mqttbridge.exceptions.InputIgnoredException;

/**
 * Passes a projector text value (model name) through unchanged, apart from trimming.
 */
public class BenqStringAdapter implements DeviceAdapter<String, String> {

    @Override
    public String parseDevice(String deviceValue) throws InputIgnoredException {
        if (deviceValue == null || deviceValue.isBlank())
            throw new InputIgnoredException();
        return deviceValue.trim();
    }

    @Override
    public String toDevice(String fieldValue) {
        return fieldValue;
    }
}
