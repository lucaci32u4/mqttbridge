package net.lucaciresearch.mqttbridge.device;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import net.lucaciresearch.mqttbridge.data.VariableNode;
import net.lucaciresearch.mqttbridge.util.ConnectionManager;

import java.util.List;
import java.util.function.Supplier;

public abstract class AbstractConnectionDeviceCallInterface<DTy> extends AbstractConnectionlessDeviceCallInterface<DTy> {

    private final ConnectionManager connectionManager = new ConnectionManager();

    protected AbstractConnectionDeviceCallInterface(List<VariableNode<?, DTy>> list, Supplier<Boolean> creator, Runnable destroyer) {
        super(list);
        connectionManager.baseDelay(2000);
        connectionManager.creator(() -> {
            if (creator.get()) {
                super.initializeConnection();
                return true;
            }
            return false;
        });
        connectionManager.destroyer(() -> {
            super.closeConnection();
            destroyer.run();
        });
    }

    @Override
    public void closeConnection() {
        connectionManager.stop();
    }

    @Override
    public void initializeConnection() {
        connectionManager.start();
    }

    protected void markConnectionAsFailed() {
        connectionManager.markFailed();
    }

}
