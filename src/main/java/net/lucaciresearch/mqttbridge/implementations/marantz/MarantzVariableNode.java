package net.lucaciresearch.mqttbridge.implementations.marantz;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.lucaciresearch.mqttbridge.data.VariableNode;
import net.lucaciresearch.mqttbridge.data.DeviceAdapter;
import net.lucaciresearch.mqttbridge.data.MqttAdapter;
import net.lucaciresearch.mqttbridge.util.PollSpeed;
import net.lucaciresearch.mqttbridge.util.TruthSource;

@Accessors(fluent = true, chain = true)
public class MarantzVariableNode<Ty> extends VariableNode<Ty, String> {

    @Getter
    private final boolean setterExtraSpace;

    @Getter
    private final boolean getterExtraSpace;

    @Getter
    private final String getterOtherKey;

    @Getter
    private final String getterEndOfResponseString;


    public MarantzVariableNode(PollSpeed pollSpeed, DeviceAdapter<Ty, String> deviceAdapter, String deviceKey, MqttAdapter<Ty> mqttAdapter, String mqttSubtopic, boolean setterExtraSpace, boolean getterExtraSpace, String getterOtherKey) {
        this(pollSpeed, deviceAdapter, deviceKey, mqttAdapter, mqttSubtopic, setterExtraSpace, getterExtraSpace, getterOtherKey, null);
    }
    public MarantzVariableNode(PollSpeed pollSpeed, DeviceAdapter<Ty, String> deviceAdapter, String deviceKey, MqttAdapter<Ty> mqttAdapter, String mqttSubtopic, boolean setterExtraSpace, boolean getterExtraSpace, String getterOtherKey, String getterEndOfResponseString) {
        super(pollSpeed, deviceAdapter, deviceKey, mqttAdapter, mqttSubtopic);
        this.setterExtraSpace = setterExtraSpace;
        this.getterExtraSpace = getterExtraSpace;
        this.getterOtherKey = getterOtherKey;
        this.getterEndOfResponseString = getterEndOfResponseString;
    }
}
