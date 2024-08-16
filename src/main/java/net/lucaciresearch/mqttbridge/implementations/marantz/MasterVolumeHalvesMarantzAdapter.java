package net.lucaciresearch.mqttbridge.implementations.marantz;

import lombok.extern.slf4j.Slf4j;
import net.lucaciresearch.mqttbridge.data.DeviceAdapter;
import net.lucaciresearch.mqttbridge.exceptions.InputIgnoredException;

@Slf4j
public class MasterVolumeHalvesMarantzAdapter implements DeviceAdapter<Double, String> {

    @Override
    public Double parseDevice(String deviceValue) throws InputIgnoredException {
        while (deviceValue.length() < 3) {
            deviceValue += "0";
        }

        try {
            return Integer.parseInt(deviceValue) * 0.1 - 80;
        } catch (NumberFormatException nfe) {
            throw new InputIgnoredException();
        }
    }

    @Override
    public String toDevice(Double fieldValue) {
        int val = (int)(Math.round(fieldValue * 10)) + 800;
        int valfirst = val / 10;
        int valsecond = val % 10;
        String firstpart = (valfirst == 0 ? "00" : (valfirst < 10 ? "0" + valfirst : Integer.toString(valfirst)));
        String secondpart = (valsecond == 5 ? "5" : "");
        return firstpart + secondpart;
    }
}
