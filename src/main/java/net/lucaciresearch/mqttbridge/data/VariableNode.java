package net.lucaciresearch.mqttbridge.data;

import io.reactivex.rxjava3.subjects.PublishSubject;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import net.lucaciresearch.mqttbridge.exceptions.InputIgnoredException;
import net.lucaciresearch.mqttbridge.exceptions.InvalidMqttInput;
import net.lucaciresearch.mqttbridge.exceptions.VariableUnavailableException;
import net.lucaciresearch.mqttbridge.util.BetterReentrantLock;
import net.lucaciresearch.mqttbridge.util.PollSpeed;
import net.lucaciresearch.mqttbridge.util.TruthSource;

import java.util.Objects;


@Accessors(fluent = true, chain = true)
@Slf4j
public abstract class VariableNode<Ty, DTy> {

    @Getter
    private Ty value;

    @Getter
    private InfoState infoState = InfoState.UNINITIALIZED;

    @Getter
    private Availability availability = Availability.UNAVAILABLE;

    @Getter
    private PublishSubject<InfoState> infoStateStream = PublishSubject.create();

    @Getter
    private PublishSubject<Availability> availabilityStream = PublishSubject.create();

    public VariableNode(PollSpeed pollSpeed, DeviceAdapter<Ty, DTy> deviceAdapter, String deviceKey, MqttAdapter<Ty> mqttAdapter, String mqttSubtopic) {
        this.pollSpeed = pollSpeed;
        this.deviceAdapter = deviceAdapter;
        this.deviceKey = deviceKey;
        this.mqttAdapter = mqttAdapter;
        this.mqttSubtopic = mqttSubtopic;
    }

    @Getter
    private PollSpeed pollSpeed;

    @Getter
    private DeviceAdapter<Ty, DTy> deviceAdapter;

    @Getter
    private String deviceKey;

    @Getter
    private MqttAdapter<Ty> mqttAdapter;

    @Getter
    private String mqttSubtopic;

    @Getter
    private final BetterReentrantLock lock = new BetterReentrantLock();

    public VariableNode<Ty, DTy> infoState(InfoState infoState) {
        boolean runUpdate = true;
        boolean same = infoState == this.infoState;
        boolean newDirty = infoState == InfoState.DIRTY_MQTT || infoState == InfoState.DIRTY_DEVICE;
        boolean oldDirty = this.infoState == InfoState.DIRTY_MQTT || this.infoState == InfoState.DIRTY_DEVICE;
        if (same) {
            if (newDirty) runUpdate = true;
            else runUpdate = false;
        }
        this.infoState = infoState;
        if (!runUpdate)
            return this;
        infoStateStream.onNext(infoState);
        return this;
    }

    public VariableNode<Ty, DTy> availability(Availability availability) {
        if (availability == this.availability)
            return this;
        this.availability = availability;
        availabilityStream.onNext(availability);
        return this;
    }

    public String getSimpleName() {
        return mqttSubtopic.replace(" ", "_").replace("-", "_");
    }

    private void setValue(Ty value, TruthSource source) {
        if (!Objects.equals(this.value, value)) {
            this.value = value;
            if (source == TruthSource.DEVICE) infoState(InfoState.DIRTY_MQTT);
            if (source == TruthSource.MQTT) infoState(InfoState.DIRTY_DEVICE);
        }
    }

    public void parseDevice(DTy deviceValue) throws VariableUnavailableException {
        try {
            Ty value = deviceAdapter.parseDevice(deviceValue);
            setValue(value, TruthSource.DEVICE);
        } catch (InputIgnoredException e) {
            // nothing
        } catch (Exception any) {
            log.error("Parsing device value {} for variable {} failed", deviceValue, deviceKey, any);
        }
    }


    public DTy toDevice() {
        return value != null ? deviceAdapter.toDevice(value) : null;
    }


    public boolean parseMqtt(String mqttValue) {
        try {
            Ty value = mqttAdapter.parseMqtt(mqttValue);
            if (value == null)
                throw new InvalidMqttInput("result is null");
            setValue(value, TruthSource.MQTT);
            return true;
        } catch (InvalidMqttInput e) {
            log.warn("Got invalid value from MQTT for variable {}: {}", deviceKey, e.getMessage());
            return false;
        } catch (Exception any) {
            log.error("Parsing MQTT value {} for variable {} failed", mqttValue, deviceKey, any);
            return false;
        }
    }


    public String toMqtt() {
        return value != null ? mqttAdapter.toMqtt(value) : null;
    }
}
