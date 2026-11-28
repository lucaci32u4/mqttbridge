package net.lucaciresearch.mqttbridge.implementations.benq;

import lombok.experimental.Accessors;
import net.lucaciresearch.mqttbridge.data.DeviceAdapter;
import net.lucaciresearch.mqttbridge.data.MqttAdapter;
import net.lucaciresearch.mqttbridge.data.VariableNode;
import net.lucaciresearch.mqttbridge.util.PollSpeed;

/**
 * A value of a BenQ projector, read with {@code *<key>=?#} and written with {@code *<key>=<value>#}.
 */
@Accessors(fluent = true, chain = true)
public class BenqVariableNode<Ty> extends VariableNode<Ty, String> {

    public BenqVariableNode(PollSpeed pollSpeed, DeviceAdapter<Ty, String> deviceAdapter, String deviceKey, MqttAdapter<Ty> mqttAdapter, String mqttSubtopic) {
        super(pollSpeed, deviceAdapter, deviceKey, mqttAdapter, mqttSubtopic);
    }
}
