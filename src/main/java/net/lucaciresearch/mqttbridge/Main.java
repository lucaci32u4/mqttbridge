package net.lucaciresearch.mqttbridge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import net.lucaciresearch.mqttbridge.device.DeviceCallInterface;
import net.lucaciresearch.mqttbridge.device.DeviceChooseModule;
import net.lucaciresearch.mqttbridge.device.DevicePropertiesInterface;
import net.lucaciresearch.mqttbridge.util.ConfigModule;
import net.lucaciresearch.mqttbridge.util.JacksonModule;
import org.apache.logging.log4j.Level;

import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Slf4j
@CommandLine.Command(mixinStandardHelpOptions = true)
public class Main implements Callable<Integer> {

    public static void main(String[] args) throws IOException {
        System.setProperty("io.netty.tryReflectionSetAccessible", "false");
        Properties vsn = new Properties();
        vsn.load(Main.class.getClassLoader().getResourceAsStream("version.properties"));
        Main main = new Main();
        CommandLine.Model.CommandSpec commandSpec = CommandLine.Model.CommandSpec.forAnnotatedObject(main);
        commandSpec.name(vsn.getProperty("app.name"));
        commandSpec.version(vsn.getProperty("app.version"));
        CommandLine commandLine = new CommandLine(commandSpec);
        commandLine.execute(args);
    }

    @CommandLine.Option(names = { "--debug" }, description = "Print debug logging")
    boolean debug = false;

    @CommandLine.Option(names = { "--config" }, description = "Config file", required = true)
    String configfile;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Integer call() {

        if (debug) {
            Configurator.setRootLevel(Level.DEBUG);
        }

        ConfigModule<Object> module = new ConfigModule<>(configfile);
        if (!module.initialize(true, new TypeReference<>() {
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
            deviceCallInterface.getNodes().stream().flatMap(n -> Stream.of(n.mqttAdapter(), n.deviceAdapter())).forEach(injector::injectMembers);
            manager.start();
        }

        return 0;
    }

}
