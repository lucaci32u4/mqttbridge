package net.lucaciresearch.mqttbridge.device;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import net.lucaciresearch.mqttbridge.data.VariableNode;

import java.util.List;

public abstract class AbstractConnectionlessDeviceCallInterface<DTy> implements DeviceCallInterface<DTy> {

    private final List<VariableNode<?, DTy>> list;
    private final PublishSubject<Boolean> openStream = PublishSubject.create();
    private boolean open = false;

    protected AbstractConnectionlessDeviceCallInterface(List<VariableNode<?, DTy>> list) {
        this.list = list;
    }

    @Override
    public Observable<Boolean> isOpenStream() {
        return openStream;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public List<VariableNode<?, DTy>> getNodes() {
        return list;
    }

    @Override
    public void closeConnection() {
        open = false;
        openStream.onNext(false);
    }

    @Override
    public void initializeConnection() {
        open = true;
        openStream.onNext(true);
    }

}
