package net.lucaciresearch.mqttbridge.implementations.demo;

import lombok.Getter;
import net.lucaciresearch.mqttbridge.data.DeviceAdapter;
import net.lucaciresearch.mqttbridge.data.MqttAdapter;
import net.lucaciresearch.mqttbridge.data.VariableNode;
import net.lucaciresearch.mqttbridge.util.PollSpeed;

import java.time.LocalTime;

public class LinuxTimeVariableNode<Ty> extends VariableNode<Ty, String> {

    @Getter
    private final String setFormat;

    @Getter
    private final String getFormat;

    public LinuxTimeVariableNode(PollSpeed pollSpeed, DeviceAdapter<Ty, String> deviceAdapter, String deviceKey, MqttAdapter<Ty> mqttAdapter, String mqttSubtopic, String setFormat, String getFormat) {
        super(pollSpeed, deviceAdapter, deviceKey, mqttAdapter, mqttSubtopic);
        this.setFormat = setFormat;
        this.getFormat = getFormat;
    }
}
