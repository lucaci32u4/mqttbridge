package net.lucaciresearch.mqttbridge.implementations.benq;

import net.lucaciresearch.mqttbridge.device.DeviceChooseModule;
import net.lucaciresearch.mqttbridge.device.DevicePropertiesInterface;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class BenqDeviceChooseModuleTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private DevicePropertiesInterface<?, ?> choose(String codename) throws Exception {
        File config = folder.newFile(codename + ".json");
        Files.writeString(config.toPath(), "{\n" +
                "  \"mqtt\": {\"username\": \"u\", \"password\": \"p\", \"port\": 1883, \"qos\": 1,\n" +
                "           \"host\": \"localhost\", \"publisherId\": \"benq\", \"baseTopic\": \"benq2mqtt/test\"},\n" +
                "  \"device\": {\"port\": \"/dev/ttyUSB0\", \"baud\": 9600},\n" +
                "  \"deviceCodename\": \"" + codename + "\"\n" +
                "}\n");
        DeviceChooseModule module = new DeviceChooseModule();
        assertTrue(codename + " not accepted", module.initialize(codename, config.getAbsolutePath()));
        assertNotNull(module.getRealConfigModule());
        return module.getDevicePropertiesInterface();
    }

    @Test
    public void bothCodenamesBuildTheSameDevice() throws Exception {
        DevicePropertiesInterface<?, ?> w2700 = choose("BenqW2700Serial");
        DevicePropertiesInterface<?, ?> ht3550 = choose("BenqHT3550Serial");
        assertTrue(w2700 instanceof BenqW2700DeviceImplementation);
        assertTrue(ht3550 instanceof BenqW2700DeviceImplementation);
        assertEquals(w2700.getManufacturer(), ht3550.getManufacturer());
        assertEquals(w2700.getModel(), ht3550.getModel());
        assertEquals(
                ((BenqW2700DeviceImplementation) w2700).getBenqDuplexInterface().getNodes().stream().map(n -> n.mqttSubtopic()).toList(),
                ((BenqW2700DeviceImplementation) ht3550).getBenqDuplexInterface().getNodes().stream().map(n -> n.mqttSubtopic()).toList());
    }

    @Test
    public void unknownBenqCodenameIsRejected() throws Exception {
        File config = folder.newFile("x.json");
        Files.writeString(config.toPath(), "{}");
        assertFalse(new DeviceChooseModule().initialize("BenqW1070Serial", config.getAbsolutePath()));
    }
}
