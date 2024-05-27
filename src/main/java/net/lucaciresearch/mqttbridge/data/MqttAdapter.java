package net.lucaciresearch.mqttbridge.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.lucaciresearch.mqttbridge.exceptions.InvalidMqttInput;
import net.lucaciresearch.mqttbridge.mqtt.HAClass;

import javax.naming.OperationNotSupportedException;

public interface MqttAdapter<Ty> {

    /**
     * Convert from MQTT input to variable type
     * @param mqttValue string from MQTT
     * @return variable value
     * @throws InvalidMqttInput of the input is invalid (bad format, bad data, in some cases also null)
     */
    Ty parseMqtt(String mqttValue) throws InvalidMqttInput;

    /**
     * Convert from variable type to MQTT string
     * @param value value of the variable
     * @return converted variable to MQTT string
     */
    String toMqtt(Ty value);

    /**
     * This method is called before discovery json is published to MQTT. Use it to add fields to the object to configure your entity.
     * @param json the root of the json
     * @throws OperationNotSupportedException if this variable is not compatible with Home Assistant MQTT discovery protocol
     */
    void addMqttDiscovery(ObjectNode json) throws OperationNotSupportedException;

    /**
     * Get the Home Assistant class of this entity
     * @return the Home Assistant entity class
     * @throws OperationNotSupportedException if this adapter is not compatible with Home Assistant MQTT discovery protocol
     */
    HAClass getHAClass() throws OperationNotSupportedException;

    /**
     * Get the Home Assistant name of this entity
     * @return the Home Assistant entity name
     * @throws OperationNotSupportedException if this adapter is not compatible with Home Assistant MQTT discovery protocol
     */
    String getHANiceName() throws OperationNotSupportedException;

}
