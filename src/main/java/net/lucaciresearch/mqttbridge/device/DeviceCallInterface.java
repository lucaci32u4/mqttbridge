package net.lucaciresearch.mqttbridge.device;

import io.reactivex.rxjava3.core.Observable;
import lombok.*;
import lombok.experimental.Accessors;
import net.lucaciresearch.mqttbridge.data.VariableNode;
import net.lucaciresearch.mqttbridge.exceptions.CallFailException;
import net.lucaciresearch.mqttbridge.exceptions.ConnectionFailedException;

import java.util.List;

public interface DeviceCallInterface<DTy> {

    void initializeConnection();

    List<VariableNode<?, DTy>> getNodes();

    boolean isOpen();

    DTy readValue(String deviceKey, boolean fastFail) throws ConnectionFailedException, CallFailException;

    DTy writeValue(String deviceKey, DTy deviceValue, boolean fastFail) throws ConnectionFailedException, CallFailException;

    Observable<Boolean> isOpenStream();

    Observable<KeyValue<DTy>> notifyValue();

    void closeConnection();




    @NoArgsConstructor
    @Getter
    @Setter
    @Accessors(fluent = true, chain = true)
    @AllArgsConstructor
    @ToString
    @EqualsAndHashCode
    class KeyValue<DTy> {
        private String key;
        private DTy value;
    }

}
