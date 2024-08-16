package net.lucaciresearch.mqttbridge.implementations.marantz;

import lombok.extern.slf4j.Slf4j;
import net.lucaciresearch.mqttbridge.data.DeviceAdapter;

@Slf4j
public class NumberLevelMarantzAdapter implements DeviceAdapter<Integer, String> {

    private final int digits;

    public NumberLevelMarantzAdapter(int digits) {
        this.digits = digits;
    }

    @Override
    public Integer parseDevice(String deviceValue) {
        return Integer.parseInt(deviceValue);
    }

    @Override
    public String toDevice(Integer fieldValue) {
        StringBuilder val = new StringBuilder(Integer.toString(fieldValue));
        while (val.length() < digits) {
            val.insert(0, "0");
        }
        return val.toString();
    }
}
