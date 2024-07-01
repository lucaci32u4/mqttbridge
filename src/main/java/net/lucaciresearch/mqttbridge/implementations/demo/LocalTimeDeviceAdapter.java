package net.lucaciresearch.mqttbridge.implementations.demo;

import net.lucaciresearch.mqttbridge.data.DeviceAdapter;
import net.lucaciresearch.mqttbridge.exceptions.VariableUnavailableException;

import java.time.LocalTime;

public class LocalTimeDeviceAdapter implements DeviceAdapter<LocalTime, LocalTime> {

    @Override
    public LocalTime parseDevice(LocalTime deviceValue) throws VariableUnavailableException {
        return deviceValue;
    }

    @Override
    public LocalTime toDevice(LocalTime fieldValue) {
        return fieldValue;
    }
}
