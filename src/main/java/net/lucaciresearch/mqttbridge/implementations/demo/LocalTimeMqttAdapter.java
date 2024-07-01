package net.lucaciresearch.mqttbridge.implementations.demo;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.lucaciresearch.mqttbridge.data.MqttAdapter;
import net.lucaciresearch.mqttbridge.exceptions.InvalidMqttInput;
import net.lucaciresearch.mqttbridge.mqtt.HAClass;

import javax.naming.OperationNotSupportedException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LocalTimeMqttAdapter implements MqttAdapter<LocalTime> {

    @Override
    public LocalTime parseMqtt(String mqttValue) throws InvalidMqttInput {
        try {
            return LocalTime.from(DateTimeFormatter.ISO_LOCAL_TIME.parse(mqttValue));
        } catch (Exception e) {
            throw new InvalidMqttInput("Invalid MQTT value: " + mqttValue);
        }
    }

    @Override
    public String toMqtt(LocalTime value) {
        return DateTimeFormatter.ISO_LOCAL_TIME.format(value);
    }

    @Override
    public void addMqttDiscovery(ObjectNode json) throws OperationNotSupportedException {
        throw new OperationNotSupportedException("Not supported");
    }

    @Override
    public HAClass getHAClass() throws OperationNotSupportedException {
        throw new OperationNotSupportedException("Not supported");
    }

    @Override
    public String getHANiceName() throws OperationNotSupportedException {
        throw new OperationNotSupportedException("Not supported");
    }
}
