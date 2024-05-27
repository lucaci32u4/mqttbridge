package net.lucaciresearch.mqttbridge.mqtt;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true, chain = true)
public class MqttConfig {

    @NotNull
    @Size(min = 1)
    String username = "homeassistant";

    @NotNull @Size(min = 1)
    String password = "aafef6a8-19d6-43a2-93e5-2f59bad55ced";

    @NotNull @Min(1) @Max(65535)
    Integer port = 1883;

    @NotNull @Size(min = 1)
    String host = "hass.lr";

    @NotNull @Min(0) @Max(2)
    Integer qos = 2;

    @NotNull @Size(min = 1)
    String publisherId = "Marantz2Mqtt";

    @NotNull @Size(min = 1)
    String baseTopic = "marantz2mqtt/livingroom";

}
