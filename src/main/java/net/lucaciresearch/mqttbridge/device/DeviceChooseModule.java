package net.lucaciresearch.mqttbridge.device;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import net.lucaciresearch.mqttbridge.implementations.marantzsr6010.MarantzTelnetConfig;
import net.lucaciresearch.mqttbridge.implementations.marantzsr6010.SR6010DeviceImplementation;
import net.lucaciresearch.mqttbridge.implementations.pipewire.PipewireConfig;
import net.lucaciresearch.mqttbridge.implementations.pipewire.PipewireDeviceImplementation;
import net.lucaciresearch.mqttbridge.implementations.util.TcpConnectionHolder;
import net.lucaciresearch.mqttbridge.util.Config;
import net.lucaciresearch.mqttbridge.util.ConfigModule;

@Slf4j
public class DeviceChooseModule extends AbstractModule {

    private ConfigModule<?> realConfigModule;

    private DevicePropertiesInterface<?, ?> devicePropertiesInterface;

    public boolean initialize(String deviceCodename, String configFile) {
        if (deviceCodename.equals("SR6010Telnet")) {
            ConfigModule<MarantzTelnetConfig> config = new ConfigModule<>(configFile);
            if (!config.initialize(false, new TypeReference<Config<MarantzTelnetConfig>>() {}))
                return false;
            realConfigModule = config;

            // Here we instantiate the connection holder before anything else because only here we can distinguish between Telnet and Serial
            devicePropertiesInterface = new SR6010DeviceImplementation(config.getDevice(), new TcpConnectionHolder(config.getDevice().host(), 23, 3000));
        }
        if (deviceCodename.equals("PipewireFilterChain")) {
            ConfigModule<PipewireConfig> config = new ConfigModule<>(configFile);
            if (!config.initialize(false, new TypeReference<Config<PipewireConfig>>() {}))
                return false;
            realConfigModule = config;
            devicePropertiesInterface = new PipewireDeviceImplementation(config.getDevice());
        }
        if (realConfigModule == null || devicePropertiesInterface == null) {
            log.error("Device {} not known or failed to initialize device config", deviceCodename);
            return false;
        }
        log.info("Device successfully chosen as {}", deviceCodename);
        return true;
    }

    @Provides
    public ConfigModule<?> getRealConfigModule() {
        return realConfigModule;
    }

    @Provides @Named("devicePropertiesInterface")
    public DevicePropertiesInterface<?, ?> getDevicePropertiesInterface() {
        return devicePropertiesInterface;
    }
}
