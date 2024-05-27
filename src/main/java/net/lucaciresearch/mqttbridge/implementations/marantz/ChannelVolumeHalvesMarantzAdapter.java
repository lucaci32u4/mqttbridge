package net.lucaciresearch.mqttbridge.implementations.marantz;

import lombok.extern.slf4j.Slf4j;
import net.lucaciresearch.mqttbridge.data.DeviceAdapter;

@Slf4j
public class ChannelVolumeHalvesMarantzAdapter implements DeviceAdapter<Double, String> {

    @Override
    public Double parseDevice(String deviceValue) {

        if (deviceValue.equals("OFF")) {
            log.error("DEV ERROR: Received OFF in number value");
            return 0.0;
        }

        if (deviceValue.equals("00")) {
            return null;
        }

        while (deviceValue.length() < 3) {
            deviceValue += "0";
        }

        return Integer.parseInt(deviceValue) * 0.1 - 50;
    }

    @Override
    public String toDevice(Double fieldValue) {
        int val = (int)(Math.round(fieldValue * 10)) + 500;
        int valfirst = val / 10;
        int valsecond = val % 10;
        String firstpart = (valfirst == 0 ? "00" : (valfirst < 10 ? "0" + valfirst : Integer.toString(valfirst)));
        String secondpart = (valsecond == 5 ? "5" : "");
        return firstpart + secondpart;
    }
}
