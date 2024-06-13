package net.lucaciresearch.mqttbridge.implementations.pipewire;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.lucaciresearch.mqttbridge.data.DeviceAdapter;
import net.lucaciresearch.mqttbridge.data.MqttAdapter;
import net.lucaciresearch.mqttbridge.data.VariableNode;
import net.lucaciresearch.mqttbridge.exceptions.InvalidMqttInput;
import net.lucaciresearch.mqttbridge.exceptions.VariableUnavailableException;
import net.lucaciresearch.mqttbridge.mqtt.HAClass;
import net.lucaciresearch.mqttbridge.util.PollSpeed;

import javax.naming.OperationNotSupportedException;
import java.util.Map;

public class PipewireVariableNode extends VariableNode<Map<String, Float>, Map<String, Float>> {


public PipewireVariableNode(PollSpeed pollSpeed, String filterName, Map<String, Float> min, Map<String, Float> max, String filterChain) {
        super(pollSpeed, new PipewireDeviceAdapter(), filterChain + "/" + filterName, new PipewireMqttAdapter(min, max), filterChain + "/" + filterName);
    }

    public static class PipewireDeviceAdapter implements DeviceAdapter<Map<String, Float>, Map<String, Float>> {
        @Override
        public Map<String, Float> parseDevice(Map<String, Float> deviceValue) throws VariableUnavailableException {
            return deviceValue;
        }

        @Override
        public Map<String, Float> toDevice(Map<String, Float> fieldValue) {
            return fieldValue;
        }
    }

    @Slf4j
    public static class PipewireMqttAdapter implements MqttAdapter<Map<String, Float>> {

        private final Map<String, Float> min;
        private final Map<String, Float> max;

        public PipewireMqttAdapter(Map<String, Float> min, Map<String, Float> max) {
            this.min = min;
            this.max = max;
        }

        @Inject
        private ObjectReader reader;

        @Inject
        private ObjectWriter writer;


        @Override
        public Map<String, Float> parseMqtt(String mqttValue) throws InvalidMqttInput {
            try {
                Map<String, Float> res = reader.readValue(mqttValue);
                if (res.size() < min.size())
                    throw new InvalidMqttInput("Missing filter properties");
                for (String key : res.keySet()) {
                    if (!min.containsKey(key) || !max.containsKey(key))
                        throw new InvalidMqttInput("Filter property " + key + " does not exist");
                    if (min.get(key) > res.get(key) || res.get(key) > max.get(key))
                        throw new InvalidMqttInput("Filter property " + key + " is outside of bounds [" + min.get(key) + ", " + max.get(key) + "]");
                }
                return res;
            } catch (JsonProcessingException e) {
                throw new InvalidMqttInput("Cannot deserialize input: " + e.getMessage());
            }
        }

        @Override
        public String toMqtt(Map<String, Float> value) {
            try {
                return writer.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                log.error("Got jackson exception while serializing pipewire filter data", e);
            }
            return "";
        }

        @Override
        public void addMqttDiscovery(ObjectNode json) throws OperationNotSupportedException {
            throw new OperationNotSupportedException("Unsupported");
        }

        @Override
        public HAClass getHAClass() throws OperationNotSupportedException {
            throw new OperationNotSupportedException("Unsupported");
        }

        @Override
        public String getHANiceName() throws OperationNotSupportedException {
            throw new OperationNotSupportedException("Unsupported");
        }
    }

}
