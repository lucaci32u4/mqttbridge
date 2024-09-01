package net.lucaciresearch.mqttbridge.implementations.demo;

import net.lucaciresearch.mqttbridge.data.DeviceAdapter;
import net.lucaciresearch.mqttbridge.exceptions.VariableUnavailableException;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimeDeviceAdapter implements DeviceAdapter<LocalTime, String> {

    @Override
    public LocalTime parseDevice(String deviceValue) throws VariableUnavailableException {
        return LocalTime.parse(deviceValue, DateTimeFormatter.ISO_LOCAL_TIME);
    }

    @Override
    public String toDevice(LocalTime fieldValue) {
        return DateTimeFormatter.ISO_LOCAL_TIME.format(fieldValue);
    }
}
