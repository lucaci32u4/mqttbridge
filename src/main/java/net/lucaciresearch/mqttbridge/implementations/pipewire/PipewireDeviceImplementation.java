package net.lucaciresearch.mqttbridge.implementations.pipewire;

import net.lucaciresearch.mqttbridge.device.DeviceCallInterface;
import net.lucaciresearch.mqttbridge.device.DevicePropertiesInterface;
import net.lucaciresearch.mqttbridge.implementations.marantzsr6010.MarantzTelnetConfig;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class PipewireDeviceImplementation implements DevicePropertiesInterface<Map<String, Float>, MarantzTelnetConfig> {

    private final PipewireConfig config;

    private final PipewireShellInterface interf;

    public PipewireDeviceImplementation(PipewireConfig config) {
        this.config = config;
        interf = new PipewireShellInterface(config.filterChains());
        try {
            interf.discoverFilters();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<DeviceCallInterface<Map<String, Float>>> getCallInterface() {
        return List.of(interf);
    }

    @Override
    public String getManufacturer() {
        return "FreeDesktop.org";
    }

    @Override
    public String getModel() {
        return "Pipewire";
    }
}
