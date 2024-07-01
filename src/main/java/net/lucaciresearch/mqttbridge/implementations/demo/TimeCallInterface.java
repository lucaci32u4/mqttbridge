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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
public class TimeCallInterface extends AbstractConnectionlessDeviceCallInterface<LocalTime> {

    private final PublishSubject<KeyValue<LocalTime>> notifications = PublishSubject.create();

    public TimeCallInterface(List<VariableNode<?, LocalTime>> list) {
        super(list);

        // Emulate receiving notifications by polling the timer regularly
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    notifications.onNext(new KeyValue<>("currentTime", readValue("currentTime", false)));
                } catch (CallFailException e) {
                    log.error("Error while generating notification", e);
                }
            }
        }, 5000, 500);
    }

    @Override
    public LocalTime readValue(String deviceKey, boolean fastFail) throws ConnectionFailedException, CallFailException {
        if (deviceKey.equals("currentTime")) {
            try {
                return LocalTime.parse(Util.executeCommand(List.of("date", "+%H:%M:%S"), 1000));
            } catch (IOException e) {
                throw new CallFailException("Failed to execute date command");
            }
        }
        throw new ConnectionFailedException("No such device key");
    }

    @Override
    public LocalTime writeValue(String deviceKey, LocalTime deviceValue, boolean fastFail) throws ConnectionFailedException, CallFailException {
        if (deviceKey.equals("currentTime")) {
            try {
                return LocalTime.parse(Util.executeCommand(List.of("sudo", "date", "--set", deviceValue.format(DateTimeFormatter.ISO_LOCAL_TIME)), 1000));
            } catch (IOException e) {
                throw new CallFailException("Failed to execute date command");
            }
        }
        throw new ConnectionFailedException("No such device key");
    }

    @Override
    public Observable<KeyValue<LocalTime>> notifyValue() {
        return notifications;
    }
}
