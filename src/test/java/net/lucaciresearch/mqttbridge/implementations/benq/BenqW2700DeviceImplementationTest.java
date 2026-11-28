package net.lucaciresearch.mqttbridge.implementations.benq;

import net.lucaciresearch.mqttbridge.data.VariableNode;
import net.lucaciresearch.mqttbridge.mqtt.HAClass;
import net.lucaciresearch.mqttbridge.util.PollSpeed;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class BenqW2700DeviceImplementationTest {

    private static VariableNode<?, String> node(List<VariableNode<?, String>> nodes, String key) {
        return nodes.stream().filter(n -> n.deviceKey().equals(key)).findFirst().orElseThrow();
    }

    @Test
    public void entityListMatchesRequest() throws Exception {
        List<VariableNode<?, String>> nodes = BenqW2700DeviceImplementation.buildNodes();
        // key -> subtopic, class, poll speed
        Map<String, Object[]> expected = new LinkedHashMap<>();
        expected.put("pow", new Object[]{"power", HAClass.SWITCH, PollSpeed.NORMAL, "Power"});
        expected.put("mute", new Object[]{"mute", HAClass.SWITCH, PollSpeed.SLOW, "Mute"});
        expected.put("blank", new Object[]{"blank", HAClass.SWITCH, PollSpeed.SLOW, "Blank"});
        expected.put("freeze", new Object[]{"freeze", HAClass.SWITCH, PollSpeed.SLOW, "Freeze"});
        expected.put("sour", new Object[]{"source", HAClass.SELECT, PollSpeed.SLOW, "Source"});
        expected.put("appmod", new Object[]{"picture-mode", HAClass.SELECT, PollSpeed.SLOW, "Picture Mode"});
        expected.put("asp", new Object[]{"aspect", HAClass.SELECT, PollSpeed.SLOW, "Aspect"});
        expected.put("lampm", new Object[]{"lamp-mode", HAClass.SELECT, PollSpeed.SLOW, "Lamp Mode"});
        expected.put("vol", new Object[]{"volume", HAClass.SENSOR, PollSpeed.SLOW, "Volume"});
        expected.put("ltim", new Object[]{"lamp-hours", HAClass.SENSOR, PollSpeed.ALMOST_NEVER, "Lamp Hours"});
        expected.put("modelname", new Object[]{"model-name", HAClass.SENSOR, PollSpeed.ALMOST_NEVER, "Model Name"});

        assertEquals(expected.size(), nodes.size());
        for (Map.Entry<String, Object[]> e : expected.entrySet()) {
            VariableNode<?, String> n = node(nodes, e.getKey());
            assertEquals(e.getKey(), e.getValue()[0], n.mqttSubtopic());
            assertEquals(e.getKey(), e.getValue()[1], n.mqttAdapter().getHAClass());
            assertEquals(e.getKey(), e.getValue()[2], n.pollSpeed());
            assertEquals(e.getKey(), e.getValue()[3], n.mqttAdapter().getHANiceName());
        }
    }

    @Test
    public void selectOptionsMapBetweenHomeAssistantAndProjector() {
        List<VariableNode<?, String>> nodes = BenqW2700DeviceImplementation.buildNodes();
        assertMqttToDevice(nodes, "sour", "HDMI 2", "hdmi2");
        assertMqttToDevice(nodes, "sour", "USB Reader", "usbreader");
        assertMqttToDevice(nodes, "appmod", "D. Cinema", "d.cinema");
        assertMqttToDevice(nodes, "appmod", "3D", "threed");
        assertMqttToDevice(nodes, "asp", "Auto", "AUTO");
        assertMqttToDevice(nodes, "asp", "Real", "REAL");
        assertMqttToDevice(nodes, "asp", "16:9", "16:9");
        assertMqttToDevice(nodes, "lampm", "SmartEco", "seco");
        assertMqttToDevice(nodes, "pow", "ON", "on");
        assertMqttToDevice(nodes, "mute", "OFF", "off");
    }

    private static void assertMqttToDevice(List<VariableNode<?, String>> nodes, String key, String mqtt, String device) {
        VariableNode<?, String> n = node(nodes, key);
        assertTrue(n.parseMqtt(mqtt));
        assertEquals(device, n.toDevice());
    }

    @Test
    public void projectorValuesShowUpInHomeAssistant() throws Exception {
        List<VariableNode<?, String>> nodes = BenqW2700DeviceImplementation.buildNodes();
        assertDeviceToMqtt(nodes, "sour", "HDMI", "HDMI 1");
        assertDeviceToMqtt(nodes, "appmod", "ISFNIGHT", "ISF Night");
        assertDeviceToMqtt(nodes, "asp", "auto", "Auto");
        assertDeviceToMqtt(nodes, "lampm", "LNOR", "Normal");
        assertDeviceToMqtt(nodes, "pow", "ON", "ON");
        assertDeviceToMqtt(nodes, "ltim", "1234", "1234");
        assertDeviceToMqtt(nodes, "vol", "5", "5");
        assertDeviceToMqtt(nodes, "modelname", "W2700", "W2700");
    }

    private static void assertDeviceToMqtt(List<VariableNode<?, String>> nodes, String key, String device, String mqtt) throws Exception {
        VariableNode<?, String> n = node(nodes, key);
        n.parseDevice(device);
        assertEquals(mqtt, n.toMqtt());
    }

    @Test
    public void unknownEnumValueKeepsLastValue() throws Exception {
        VariableNode<?, String> n = node(BenqW2700DeviceImplementation.buildNodes(), "sour");
        n.parseDevice("HDMI2");
        n.parseDevice("RGB");
        assertEquals("HDMI 2", n.toMqtt());
    }

    @Test
    public void sensorsRejectMqttWrites() {
        List<VariableNode<?, String>> nodes = BenqW2700DeviceImplementation.buildNodes();
        assertFalse(node(nodes, "ltim").parseMqtt("10"));
        assertFalse(node(nodes, "modelname").parseMqtt("x"));
        assertFalse(node(nodes, "vol").parseMqtt("3"));
    }

    @Test
    public void writingAspectAutoSendsUppercase() throws Exception {
        FakeDuplexConnectionHolder fake = new FakeDuplexConnectionHolder().reply("\r*asp=AUTO#\r", ">*asp=AUTO#\r*ASP=AUTO#\r");
        BenqW2700DeviceImplementation device = new BenqW2700DeviceImplementation(fake);
        BenqDuplexInterface iface = device.getBenqDuplexInterface();
        iface.initializeConnection();
        try {
            long deadline = System.currentTimeMillis() + 2000;
            while (!iface.isOpen() && System.currentTimeMillis() < deadline)
                Thread.sleep(10);
            VariableNode<?, String> asp = node(iface.getNodes(), "asp");
            assertTrue(asp.parseMqtt("Auto"));
            assertEquals("AUTO", iface.writeValue("asp", asp.toDevice(), false));
            assertEquals(List.of("\r*asp=AUTO#\r"), fake.written());
        } finally {
            iface.closeConnection();
        }
    }

    @Test
    public void metadata() {
        BenqW2700DeviceImplementation device = new BenqW2700DeviceImplementation(new FakeDuplexConnectionHolder());
        assertEquals("BenQ", device.getManufacturer());
        assertEquals("W2700 / HT3550", device.getModel());
        assertEquals(1, device.getCallInterface().size());
    }
}
