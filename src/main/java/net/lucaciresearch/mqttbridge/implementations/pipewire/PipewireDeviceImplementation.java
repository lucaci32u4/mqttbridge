package net.lucaciresearch.mqttbridge.implementations.pipewire;

import net.lucaciresearch.mqttbridge.device.DeviceCallInterface;
import net.lucaciresearch.mqttbridge.device.DevicePropertiesInterface;
import net.lucaciresearch.mqttbridge.implementations.marantzsr6010.MarantzTelnetConfig;

import java.util.List;
import java.util.Map;

public class PipewireDeviceImplementation implements DevicePropertiesInterface<Map<String, Float>, MarantzTelnetConfig> {

    private final PipewireConfig config;

    public PipewireDeviceImplementation(PipewireConfig config) {
        this.config = config;
    }

    @Override
    public List<DeviceCallInterface<Map<String, Float>>> getCallInterface() {
        return List.of(new PipewireShellInterface(config.filterChains()));
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
