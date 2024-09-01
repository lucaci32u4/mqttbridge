package net.lucaciresearch.mqttbridge.implementations.demo;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.lucaciresearch.mqttbridge.data.MqttAdapter;
import net.lucaciresearch.mqttbridge.exceptions.InvalidMqttInput;
import net.lucaciresearch.mqttbridge.mqtt.HAClass;

import javax.naming.OperationNotSupportedException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LocalDateMqttAdapter implements MqttAdapter<LocalDate> {

    @Override
    public LocalDate parseMqtt(String mqttValue) throws InvalidMqttInput {
        try {
            return LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(mqttValue));
        } catch (Exception e) {
            throw new InvalidMqttInput("Invalid MQTT value: " + mqttValue);
        }
    }

    @Override
    public String toMqtt(LocalDate value) {
        return DateTimeFormatter.ISO_LOCAL_DATE.format(value);
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
