package net.lucaciresearch.mqttbridge.mqtt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@AllArgsConstructor
@Accessors(fluent = true, chain = true)
public enum HAClass {
    SENSOR("sensor"), BINARY_SENSOR("binary_sensor"),
    UPDATE("update"), DEVICE_AUTOMATION("device_automation"),
    SELECT("select"), SWITCH("switch"),
    NUMBER("number"), LIGHT("light")

    ;

    private final String haName;
}
