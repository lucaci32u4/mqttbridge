package net.lucaciresearch.mqttbridge.device;

import java.util.List;

public interface DevicePropertiesInterface<DTy, DCTy> {

    List<DeviceCallInterface<DTy>> getCallInterface();

    String getManufacturer();

    String getModel();

}
