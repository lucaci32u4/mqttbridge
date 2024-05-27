package net.lucaciresearch.mqttbridge.data;

import net.lucaciresearch.mqttbridge.exceptions.VariableUnavailableException;

public interface DeviceAdapter<Ty, DTy> {

     /**
      * Convert from device format to variable type
      * @param deviceValue value obtained from device
      * @return converted value
      * @throws VariableUnavailableException if the device reported that the variable is disabled, temporary unavailable or otherwise unusable.
      */
     Ty parseDevice(DTy deviceValue) throws VariableUnavailableException;

     /**
      * Convert from variable type to device format
      * @param fieldValue value of the variable
      * @return converted value
      */
     DTy toDevice(Ty fieldValue);

}
