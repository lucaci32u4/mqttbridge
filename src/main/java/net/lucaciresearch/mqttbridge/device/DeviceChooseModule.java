package net.lucaciresearch.mqttbridge.device;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import net.lucaciresearch.mqttbridge.implementations.demo.TimeDeviceProperties;
import net.lucaciresearch.mqttbridge.implementations.marantzsr6010.MarantzSerialConfig;
import net.lucaciresearch.mqttbridge.implementations.marantzsr6010.MarantzTelnetConfig;
import net.lucaciresearch.mqttbridge.implementations.marantzsr6010.MarantzDeviceImplementation;
import net.lucaciresearch.mqttbridge.implementations.pipewire.PipewireConfig;
import net.lucaciresearch.mqttbridge.implementations.pipewire.PipewireDeviceImplementation;
import net.lucaciresearch.mqttbridge.implementations.util.DuplexConnectionHolder;
import net.lucaciresearch.mqttbridge.implementations.util.SerialConnectionHolder;
import net.lucaciresearch.mqttbridge.implementations.util.TcpConnectionHolder;
import net.lucaciresearch.mqttbridge.util.Config;
import net.lucaciresearch.mqttbridge.util.ConfigModule;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DeviceChooseModule extends AbstractModule {

    private final static Pattern marantzPattern = Pattern.compile("(SR|NR)([0-9]{4})(Telnet|Serial)");

    private ConfigModule<?> realConfigModule;

    private DevicePropertiesInterface<?, ?> devicePropertiesInterface;


    public boolean initialize(String deviceCodename, String configFile) {

        // Marantz devices
        if (handleMarantzDevices(deviceCodename, configFile))
            return true;

        // Pipewire Filter Chains
        if (deviceCodename.equals("PipewireFilterChain")) {
            ConfigModule<PipewireConfig> config = new ConfigModule<>(configFile);
            if (!config.initialize(false, new TypeReference<Config<PipewireConfig>>() {}))
                return false;
            realConfigModule = config;
            devicePropertiesInterface = new PipewireDeviceImplementation(config.getDevice());
        }

        if (deviceCodename.equals("LinuxDateTime")) {
            ConfigModule<Object> config = new ConfigModule<>(configFile);
            if (!config.initialize(false, new TypeReference<Config<Object>>() {}))
                return false;
            realConfigModule = config;
            devicePropertiesInterface = new TimeDeviceProperties();
        }

        // Default no device found
        if (realConfigModule == null || devicePropertiesInterface == null) {
            log.error("Device {} not known or failed to initialize device config", deviceCodename);
            return false;
        }
        log.info("Device successfully chosen as {}", deviceCodename);
        return true;
    }

    private boolean handleMarantzDevices(String deviceCnodename, String configFile) {
        Matcher matcher = marantzPattern.matcher(deviceCnodename);
        if (!matcher.matches())
            return false;

        String series = matcher.group(1);
        String code = matcher.group(2);
        String connectionType = matcher.group(3);

        DuplexConnectionHolder connectionHolder;
        if (connectionType.equals("Telnet")) {
            ConfigModule<MarantzTelnetConfig> config = new ConfigModule<>(configFile);
            if (!config.initialize(false, new TypeReference<Config<MarantzTelnetConfig>>() {}))
                return false;
            realConfigModule = config;
            connectionHolder = new TcpConnectionHolder(config.getDevice().host(), 23, 3000);
        } else if (connectionType.equals("Serial")) {
            ConfigModule<MarantzSerialConfig> config = new ConfigModule<>(configFile);
            if (!config.initialize(false, new TypeReference<Config<MarantzSerialConfig>>() {}))
                return false;
            realConfigModule = config;
            connectionHolder = new SerialConnectionHolder(config.getDevice().port(), config.getDevice().baud());
        } else return false;

        try {
            devicePropertiesInterface = new MarantzDeviceImplementation(connectionHolder, code, series);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
            log.error("Device {}{} is not yet supported, but support is planned. To speed up development, please open an issue on Github to let us know of your interest in this device. Community contributions are always welcome.", series, code);
            log.error("Link: https://github.com/lucaci32u4/mqttbridge/issues/new?title=Support%20for%20{}{}", series, code);
            log.error("Thank you!");
            return false;
        }

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
