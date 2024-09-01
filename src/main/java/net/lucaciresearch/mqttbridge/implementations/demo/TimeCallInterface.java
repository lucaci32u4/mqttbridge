package net.lucaciresearch.mqttbridge.implementations.demo;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import lombok.extern.slf4j.Slf4j;
import net.lucaciresearch.mqttbridge.data.VariableNode;
import net.lucaciresearch.mqttbridge.device.AbstractConnectionlessDeviceCallInterface;
import net.lucaciresearch.mqttbridge.exceptions.CallFailException;
import net.lucaciresearch.mqttbridge.exceptions.ConnectionFailedException;
import net.lucaciresearch.mqttbridge.util.Util;

import java.io.IOException;
import java.time.LocalTime;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
public class TimeCallInterface extends AbstractConnectionlessDeviceCallInterface<String> {

    private final PublishSubject<KeyValue<String>> notifications = PublishSubject.create();

    public TimeCallInterface(List<VariableNode<?, String>> list) {
        super(list);

        // Emulate receiving notifications by polling the timer regularly
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

            }
        }, 5000, 500);
    }

    @Override
    public String readValue(String deviceKey, boolean fastFail) throws ConnectionFailedException, CallFailException {
        LinuxTimeVariableNode variable = getNodes().stream().filter(dk -> dk.deviceKey().equals(deviceKey)).map(LinuxTimeVariableNode.class::cast).findFirst().orElse(null);
        try {
            return Util.executeCommand(List.of("bash", "-c", variable.getGetFormat()), 1000).replace("\n", "");
        } catch (IOException e) {
            throw new ConnectionFailedException(e.getMessage());
        }
    }

    @Override
    public String writeValue(String deviceKey, String deviceValue, boolean fastFail) throws ConnectionFailedException, CallFailException {
        LinuxTimeVariableNode variable = getNodes().stream().filter(dk -> dk.deviceKey().equals(deviceKey)).map(LinuxTimeVariableNode.class::cast).findFirst().orElse(null);
        try {
            return Util.executeCommand(List.of("bash", "-c", variable.getSetFormat().replace("{}", deviceValue)), 1000).replace("\n", "");
        } catch (IOException e) {
            throw new ConnectionFailedException(e.getMessage());
        }
    }

    @Override
    public Observable<KeyValue<String>> notifyValue() {
        return notifications;
    }
}
