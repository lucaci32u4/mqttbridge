package net.lucaciresearch.mqttbridge.implementations.marantz;

import lombok.extern.slf4j.Slf4j;
import net.lucaciresearch.mqttbridge.data.DeviceAdapter;

@Slf4j
public class OnOffAdapter implements DeviceAdapter<Boolean, String> {

    @Override
    public Boolean parseDevice(String deviceValue) {
        return deviceValue.trim().equalsIgnoreCase("on");
    }

    @Override
    public String toDevice(Boolean fieldValue) {
        return fieldValue ? "ON" : "OFF";
    }
}
