package net.lucaciresearch.mqttbridge.implementations.benq;

import lombok.extern.slf4j.Slf4j;
import net.lucaciresearch.mqttbridge.data.DeviceAdapter;
import net.lucaciresearch.mqttbridge.exceptions.InputIgnoredException;

/**
 * Converts BenQ {@code on}/{@code off} values (any case) to booleans.
 */
@Slf4j
public class BenqOnOffAdapter implements DeviceAdapter<Boolean, String> {

    @Override
    public Boolean parseDevice(String deviceValue) throws InputIgnoredException {
        String v = deviceValue == null ? "" : deviceValue.trim();
        if (v.equalsIgnoreCase("on"))
            return true;
        if (v.equalsIgnoreCase("off"))
            return false;
        log.warn("Ignoring unknown on/off value from projector: {}", deviceValue);
        throw new InputIgnoredException();
    }

    @Override
    public String toDevice(Boolean fieldValue) {
        return fieldValue ? "on" : "off";
    }
}
