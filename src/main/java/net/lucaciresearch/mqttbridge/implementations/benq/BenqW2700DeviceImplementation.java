package net.lucaciresearch.mqttbridge.implementations.benq;

import lombok.Getter;
import net.lucaciresearch.mqttbridge.data.VariableNode;
import net.lucaciresearch.mqttbridge.device.DeviceCallInterface;
import net.lucaciresearch.mqttbridge.device.DevicePropertiesInterface;
import net.lucaciresearch.mqttbridge.implementations.homeassistant.BooleanHAMqttAdapter;
import net.lucaciresearch.mqttbridge.implementations.homeassistant.EnumHACompatibleMqttAdapter;
import net.lucaciresearch.mqttbridge.implementations.homeassistant.SensorHAMqttAdapter;
import net.lucaciresearch.mqttbridge.implementations.util.DuplexConnectionHolder;
import net.lucaciresearch.mqttbridge.mqtt.HAClass;
import net.lucaciresearch.mqttbridge.util.PollSpeed;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BenQ W2700 / HT3550 projector over RS-232 (part 1: core controls).
 * Command table: protocols/Benq W2700 HT3550.pdf, pages 8 to 16, rows marked YES.
 */
@Getter
public class BenqW2700DeviceImplementation implements DevicePropertiesInterface<String, BenqSerialConfig> {

    private final DuplexConnectionHolder connectionHolder;

    private final BenqDuplexInterface benqDuplexInterface;

    public BenqW2700DeviceImplementation(DuplexConnectionHolder connectionHolder) {
        this.connectionHolder = connectionHolder;
        this.benqDuplexInterface = new BenqDuplexInterface(connectionHolder, buildNodes());
    }

    static List<VariableNode<?, String>> buildNodes() {
        Map<String, String> sources = options(
                "hdmi", "HDMI 1",
                "hdmi2", "HDMI 2",
                "usbreader", "USB Reader");
        Map<String, String> pictureModes = options(
                "bright", "Bright",
                "vivid", "Vivid",
                "cine", "Cinema",
                "d.cinema", "D. Cinema",
                "user1", "User 1",
                "isfday", "ISF Day",
                "isfnight", "ISF Night",
                "threed", "3D");
        Map<String, String> aspects = options(
                "AUTO", "Auto",
                "4:3", "4:3",
                "16:9", "16:9",
                "REAL", "Real");
        Map<String, String> lampModes = options(
                "lnor", "Normal",
                "eco", "Eco",
                "seco", "SmartEco");

        return List.of(
                new BenqVariableNode<>(PollSpeed.NORMAL, new BenqOnOffAdapter(), "pow",
                        new BooleanHAMqttAdapter("Power", HAClass.SWITCH), "power"),
                new BenqVariableNode<>(PollSpeed.SLOW, new BenqOnOffAdapter(), "mute",
                        new BooleanHAMqttAdapter("Mute", HAClass.SWITCH), "mute"),
                new BenqVariableNode<>(PollSpeed.SLOW, new BenqOnOffAdapter(), "blank",
                        new BooleanHAMqttAdapter("Blank", HAClass.SWITCH), "blank"),
                new BenqVariableNode<>(PollSpeed.SLOW, new BenqOnOffAdapter(), "freeze",
                        new BooleanHAMqttAdapter("Freeze", HAClass.SWITCH), "freeze"),
                new BenqVariableNode<>(PollSpeed.SLOW, new BenqEnumAdapter(sources.keySet()), "sour",
                        new EnumHACompatibleMqttAdapter("Source", HAClass.SELECT, sources), "source"),
                new BenqVariableNode<>(PollSpeed.SLOW, new BenqEnumAdapter(pictureModes.keySet()), "appmod",
                        new EnumHACompatibleMqttAdapter("Picture Mode", HAClass.SELECT, pictureModes), "picture-mode"),
                new BenqVariableNode<>(PollSpeed.SLOW, new BenqEnumAdapter(aspects.keySet()), "asp",
                        new EnumHACompatibleMqttAdapter("Aspect", HAClass.SELECT, aspects), "aspect"),
                new BenqVariableNode<>(PollSpeed.SLOW, new BenqEnumAdapter(lampModes.keySet()), "lampm",
                        new EnumHACompatibleMqttAdapter("Lamp Mode", HAClass.SELECT, lampModes), "lamp-mode"),
                // No numeric volume range is published in BenQ's W2700 / HT3550 user manual,
                // so volume is exposed read-only (see protocols/support/benq-projector.md).
                new BenqVariableNode<>(PollSpeed.SLOW, new BenqIntegerAdapter(), "vol",
                        new SensorHAMqttAdapter<Integer>("Volume"), "volume"),
                new BenqVariableNode<>(PollSpeed.ALMOST_NEVER, new BenqIntegerAdapter(), "ltim",
                        new SensorHAMqttAdapter<Integer>("Lamp Hours", "h"), "lamp-hours"),
                new BenqVariableNode<>(PollSpeed.ALMOST_NEVER, new BenqStringAdapter(), "modelname",
                        new SensorHAMqttAdapter<String>("Model Name"), "model-name")
        );
    }

    private static Map<String, String> options(String... keyValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2)
            map.put(keyValues[i], keyValues[i + 1]);
        return map;
    }

    @Override
    public List<DeviceCallInterface<String>> getCallInterface() {
        return List.of(benqDuplexInterface);
    }

    @Override
    public String getManufacturer() {
        return "BenQ";
    }

    @Override
    public String getModel() {
        return "W2700 / HT3550";
    }
}
