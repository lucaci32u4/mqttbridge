package net.lucaciresearch.mqttbridge.implementations.benq;

import lombok.extern.slf4j.Slf4j;
import net.lucaciresearch.mqttbridge.data.DeviceAdapter;
import net.lucaciresearch.mqttbridge.exceptions.InputIgnoredException;

/**
 * Converts BenQ integer values (volume, lamp hours).
 */
@Slf4j
public class BenqIntegerAdapter implements DeviceAdapter<Integer, String> {

    @Override
    public Integer parseDevice(String deviceValue) throws InputIgnoredException {
        try {
            return Integer.parseInt(deviceValue.trim());
        } catch (NumberFormatException | NullPointerException e) {
            log.warn("Ignoring non-integer value from projector: {}", deviceValue);
            throw new InputIgnoredException();
        }
    }

    @Override
    public String toDevice(Integer fieldValue) {
        return Integer.toString(fieldValue);
    }
}
