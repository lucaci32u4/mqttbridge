package net.lucaciresearch.mqttbridge.implementations.benq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.lucaciresearch.mqttbridge.exceptions.InvalidMqttInput;
import net.lucaciresearch.mqttbridge.implementations.homeassistant.SensorHAMqttAdapter;
import net.lucaciresearch.mqttbridge.mqtt.HAClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class SensorHAMqttAdapterTest {

    @Test
    public void parseMqttIsReadOnly() {
        InvalidMqttInput e = assertThrows(InvalidMqttInput.class, () -> new SensorHAMqttAdapter<Integer>("Lamp Hours", "h").parseMqtt("5"));
        assertEquals("read-only", e.getMessage());
    }

    @Test
    public void formatsValue() {
        assertEquals("1234", new SensorHAMqttAdapter<Integer>("Lamp Hours", "h").toMqtt(1234));
        assertEquals("W2700", new SensorHAMqttAdapter<String>("Model Name").toMqtt("W2700"));
    }

    @Test
    public void discoveryIsSensorWithOptionalUnit() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SensorHAMqttAdapter<Integer> hours = new SensorHAMqttAdapter<>("Lamp Hours", "h");
        assertEquals(HAClass.SENSOR, hours.getHAClass());
        ObjectNode withUnit = mapper.createObjectNode().put("command_topic", "x");
        hours.addMqttDiscovery(withUnit);
        assertEquals("h", withUnit.get("unit_of_measurement").asText());
        assertFalse(withUnit.has("command_topic"));

        ObjectNode noUnit = mapper.createObjectNode();
        new SensorHAMqttAdapter<String>("Model Name").addMqttDiscovery(noUnit);
        assertFalse(noUnit.has("unit_of_measurement"));
    }
}
