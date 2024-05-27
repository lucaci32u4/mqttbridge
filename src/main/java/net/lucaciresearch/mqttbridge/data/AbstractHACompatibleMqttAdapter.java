package net.lucaciresearch.mqttbridge.data;

import com.fasterxml.jackson.databind.JsonNode;
import net.lucaciresearch.mqttbridge.mqtt.HAClass;

import javax.naming.OperationNotSupportedException;

public abstract class AbstractHACompatibleMqttAdapter<Ty> implements MqttAdapter<Ty> {

    private final String hANiceName;
    private final HAClass hAClass;

    public AbstractHACompatibleMqttAdapter(String hANiceName, HAClass hAClass) {
        this.hANiceName = hANiceName;
        this.hAClass = hAClass;
    }

    @Override
    public HAClass getHAClass() throws OperationNotSupportedException {
        return hAClass;
    }

    @Override
    public String getHANiceName() throws OperationNotSupportedException {
        return hANiceName;
    }
}
