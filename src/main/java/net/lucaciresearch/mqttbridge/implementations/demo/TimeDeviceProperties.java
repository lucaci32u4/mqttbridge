package net.lucaciresearch.mqttbridge.implementations.demo;

import net.lucaciresearch.mqttbridge.device.DeviceCallInterface;
import net.lucaciresearch.mqttbridge.device.DevicePropertiesInterface;
import net.lucaciresearch.mqttbridge.util.PollSpeed;

import java.time.LocalTime;
import java.util.List;

public class TimeDeviceProperties implements DevicePropertiesInterface<LocalTime, Object> {

    private final TimeCallInterface timeCallInterface;

    public TimeDeviceProperties() {
        timeCallInterface = new TimeCallInterface(List.of(
            new TimeVariableNode(PollSpeed.NONE, new LocalTimeDeviceAdapter(), "currentTime", new LocalTimeMqttAdapter(), "current-time")
        ));
    }

    @Override
    public List<DeviceCallInterface<LocalTime>> getCallInterface() {
        return List.of(timeCallInterface);
    }

    @Override
    public String getManufacturer() {
        return "Linux";
    }

    @Override
    public String getModel() {
        return "LocalTime";
    }
}
