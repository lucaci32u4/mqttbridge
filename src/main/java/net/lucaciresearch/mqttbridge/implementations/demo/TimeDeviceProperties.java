package net.lucaciresearch.mqttbridge.implementations.demo;

import net.lucaciresearch.mqttbridge.device.DeviceCallInterface;
import net.lucaciresearch.mqttbridge.device.DevicePropertiesInterface;
import net.lucaciresearch.mqttbridge.util.PollSpeed;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class TimeDeviceProperties implements DevicePropertiesInterface<String, Object> {

    private final TimeCallInterface timeCallInterface;

    public TimeDeviceProperties() {
        timeCallInterface = new TimeCallInterface(List.of(
            new LinuxTimeVariableNode<LocalTime>(PollSpeed.NONE, new TimeDeviceAdapter(), "currentTime", new LocalTimeMqttAdapter(), "current-time",
                    "date -s {} +'%H:%M:%S'", "date +'%H:%M:%S'"),
            new LinuxTimeVariableNode<LocalDate>(PollSpeed.NONE, new DateDeviceAdapter(), "currentDate", new LocalDateMqttAdapter(), "current-date",
                    "date -s {} +'%Y-%m-%d'", "date +'%Y-%m-%d'")
        ));
    }

    @Override
    public List<DeviceCallInterface<String>> getCallInterface() {
        return List.of(timeCallInterface);
    }

    @Override
    public String getManufacturer() {
        return "Linux";
    }

    @Override
    public String getModel() {
        return "Time";
    }
}
