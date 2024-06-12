package net.lucaciresearch.mqttbridge.implementations.pipewire;

import net.lucaciresearch.mqttbridge.device.DeviceCallInterface;
import net.lucaciresearch.mqttbridge.device.DevicePropertiesInterface;
import net.lucaciresearch.mqttbridge.implementations.marantzsr6010.MarantzTelnetConfig;

import java.util.List;

public class PipewireDeviceImplementation implements DevicePropertiesInterface<String, MarantzTelnetConfig> {

    private final PipewireConfig config;

    public PipewireDeviceImplementation(PipewireConfig config) {
        this.config = config;
    }

    @Override
    public List<DeviceCallInterface<String>> getCallInterface() {
        return null;
    }

    @Override
    public String getManufacturer() {
        return "FreeDesktop";
    }

    @Override
    public String getModel() {
        return "Pipewire";
    }
}
