package net.lucaciresearch.mqttbridge.implementations.benq;

import lombok.extern.slf4j.Slf4j;
import net.lucaciresearch.mqttbridge.data.DeviceAdapter;
import net.lucaciresearch.mqttbridge.exceptions.InputIgnoredException;

import java.util.Collection;
import java.util.List;

/**
 * Maps a projector value to one of the known option keys, ignoring case.
 * Values are sent to the projector exactly as the option key is written.
 */
@Slf4j
public class BenqEnumAdapter implements DeviceAdapter<String, String> {

    private final List<String> options;

    public BenqEnumAdapter(Collection<String> options) {
        this.options = List.copyOf(options);
    }

    @Override
    public String parseDevice(String deviceValue) throws InputIgnoredException {
        String v = deviceValue == null ? "" : deviceValue.trim();
        for (String option : options) {
            if (option.equalsIgnoreCase(v))
                return option;
        }
        log.warn("Ignoring unknown value from projector: {} (known: {})", deviceValue, options);
        throw new InputIgnoredException();
    }

    @Override
    public String toDevice(String fieldValue) {
        return fieldValue;
    }
}
