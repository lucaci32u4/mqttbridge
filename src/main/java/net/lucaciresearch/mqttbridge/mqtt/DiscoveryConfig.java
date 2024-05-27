package net.lucaciresearch.mqttbridge.mqtt;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true, chain = true)
public class DiscoveryConfig {

    @NotNull @Size(min = 1)
    String entityName = "Speakers";

    @NotNull @Size(min = 1)
    String discoveryBaseTopic = "homeassistant";



}
