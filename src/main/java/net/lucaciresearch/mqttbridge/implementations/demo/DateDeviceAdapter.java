package net.lucaciresearch.mqttbridge.implementations.demo;

import net.lucaciresearch.mqttbridge.data.DeviceAdapter;
import net.lucaciresearch.mqttbridge.exceptions.VariableUnavailableException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DateDeviceAdapter implements DeviceAdapter<LocalDate, String> {

    @Override
    public LocalDate parseDevice(String deviceValue) throws VariableUnavailableException {
        return LocalDate.parse(deviceValue, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Override
    public String toDevice(LocalDate fieldValue) {
        return DateTimeFormatter.ISO_LOCAL_DATE.format(fieldValue);
    }
}
