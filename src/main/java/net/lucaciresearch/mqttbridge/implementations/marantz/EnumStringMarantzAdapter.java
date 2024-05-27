package net.lucaciresearch.mqttbridge.implementations.marantz;

import lombok.AllArgsConstructor;
import net.lucaciresearch.mqttbridge.data.DeviceAdapter;

@AllArgsConstructor
public class EnumStringMarantzAdapter implements DeviceAdapter<String, String> {

    private final boolean splitAtNonAlphabeticCharacter;

    @Override
    public String parseDevice(String deviceValue) {
        for (int i = 0; i < deviceValue.length() && splitAtNonAlphabeticCharacter; i++) {
            if (!Character.isAlphabetic(deviceValue.charAt(i))) {
                deviceValue = deviceValue.substring(0, i);
                break;
            }
        }
        return deviceValue;
    }

    @Override
    public String toDevice(String fieldValue) {
        return fieldValue;
    }
}
