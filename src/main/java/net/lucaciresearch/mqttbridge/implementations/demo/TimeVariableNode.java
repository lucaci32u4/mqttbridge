package net.lucaciresearch.mqttbridge.implementations.demo;

import net.lucaciresearch.mqttbridge.data.DeviceAdapter;
import net.lucaciresearch.mqttbridge.data.MqttAdapter;
import net.lucaciresearch.mqttbridge.data.VariableNode;
import net.lucaciresearch.mqttbridge.util.PollSpeed;

import java.time.LocalTime;

public class TimeVariableNode extends VariableNode<LocalTime, LocalTime> {

    public TimeVariableNode(PollSpeed pollSpeed, DeviceAdapter<LocalTime, LocalTime> deviceAdapter, String deviceKey, MqttAdapter<LocalTime> mqttAdapter, String mqttSubtopic) {
        super(pollSpeed, deviceAdapter, deviceKey, mqttAdapter, mqttSubtopic);
    }

}
