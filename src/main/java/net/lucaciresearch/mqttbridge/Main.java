package net.lucaciresearch.mqttbridge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import net.lucaciresearch.mqttbridge.device.DeviceCallInterface;
import net.lucaciresearch.mqttbridge.device.DeviceChooseModule;
import net.lucaciresearch.mqttbridge.device.DevicePropertiesInterface;
import net.lucaciresearch.mqttbridge.util.Config;
import net.lucaciresearch.mqttbridge.util.ConfigModule;
import net.lucaciresearch.mqttbridge.util.JacksonModule;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(name = "MqttBridge")
public class Main implements Callable<Integer> {

    public static void main(String[] args) {
        new CommandLine(new Main()).execute(args);
    }

    @CommandLine.Option(names = { "--debug" }, description = "Print debug logging")
    boolean debug = false;

    @CommandLine.Option(names = { "--config" }, description = "Config file", required = true)
    String configfile;

    @Override
    public Integer call() throws Exception {

        if (debug) {
            Configurator.setRootLevel(Level.DEBUG);
        }

        ConfigModule<Object> module = new ConfigModule<Object>(configfile);
        if (!module.initialize(true, new TypeReference<Config<Object>>() {
        })) {
            return -1;
        }

        DeviceChooseModule deviceChooseModule = new DeviceChooseModule();
        if (!deviceChooseModule.initialize(module.getDeviceCodename(), configfile)) {
            return -1;
        }

        System.setProperty("fazecast.jSerialComm.appid", "mqttbridge" + module.getMqtt().publisherId());

        Injector injector = Guice.createInjector(
                deviceChooseModule,
                deviceChooseModule.getRealConfigModule(),
                new JacksonModule()
        );

        DevicePropertiesInterface<?, ?> props = deviceChooseModule.getDevicePropertiesInterface();
        for (DeviceCallInterface<?> deviceCallInterface : props.getCallInterface()) {
            BridgeManager<?> manager = new BridgeManager<>();
            manager.setDevicePropertiesInterface((DevicePropertiesInterface) props);
            manager.setDci((DeviceCallInterface) deviceCallInterface);
            injector.injectMembers(manager);
            manager.start();
        }

        return 0;
    }

}
