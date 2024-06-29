package net.lucaciresearch.mqttbridge.device;

import io.reactivex.rxjava3.core.Observable;
import lombok.*;
import lombok.experimental.Accessors;
import net.lucaciresearch.mqttbridge.data.VariableNode;
import net.lucaciresearch.mqttbridge.exceptions.CallFailException;
import net.lucaciresearch.mqttbridge.exceptions.ConnectionFailedException;
import net.lucaciresearch.mqttbridge.util.ConnectionManager;

import java.util.List;

public interface DeviceCallInterface<DTy> {

    /**
     * Start connecting to the device. Even if the connection fails, the {@link DeviceCallInterface} should try to re-establish the connection until {@link DeviceCallInterface#closeConnection} is called.
     * To aid in this, class {@link ConnectionManager} can be used.
     */
    void initializeConnection();

    /**
     * Signal to close the current connection to the device and stop any reconnection attempts.
     */
    void closeConnection();

    /**
     * Get the list if variables supported for the device
     * @return the list of variables
     */
    List<VariableNode<?, DTy>> getNodes();

    /**
     * Checks if the connection to the device is open
     * In the event that the connection failed, this will return false until the connection is re-established, when it should return true
     * @return true if the connection is currently open and healthy
     */
    boolean isOpen();

    /**
     * Returns an observable containing changes to the open state of the connection. Allows the application to react to temporary connection failures.
     * @return rxjava observable stream
     */
    Observable<Boolean> isOpenStream();

    /**
     * Read a variable from the device.
     * This call should be blocking and synchronous.
     * @param deviceKey the variable's identifier on the device
     * @param fastFail if true an error of type {@link CallFailException} should mark the connection as failed and (after the call returns) attempt to reconnect.
     * @return the read value
     * @throws ConnectionFailedException if the device is not connected or if the connection failed.
     * @throws CallFailException if the variable is temporarily unavailable for reading or any other unrecoverable exception occurred.
     */
    DTy readValue(String deviceKey, boolean fastFail) throws ConnectionFailedException, CallFailException;

    /**
     * Write a variable to the device.
     * This call should be blocking and synchronous.
     * @param deviceKey the variable's identifier on the device
     * @param deviceValue the new value to write
     * @param fastFail if true an error of type {@link CallFailException} should mark the connection as failed and (after the call returns) attempt to reconnect.
     * @return the new value written to the device. If the device refused the value (can happen with Marantz devices for example) this function should return the current state of the variable in the device.
     * @throws ConnectionFailedException if the device is not connected or if the connection failed.
     * @throws CallFailException if the variable is temporarily unavailable for writing or any other unrecoverable exception occurred.
     */
    DTy writeValue(String deviceKey, DTy deviceValue, boolean fastFail) throws ConnectionFailedException, CallFailException;

    /**
     * A stream of key-value pairs representing updates sent by the device of state changes.
     * @return rxjava observable stream containing {@link KeyValue} objects with the device key that changed state and its new corresponding value.
     */
    Observable<KeyValue<DTy>> notifyValue();




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
