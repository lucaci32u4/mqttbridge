package net.lucaciresearch.mqttbridge.mqtt;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Map;

@Getter @Setter
@AllArgsConstructor
@Accessors(chain = true, fluent = true)
public class HAAvailability {

    @JsonProperty("system")
    boolean system;

    @JsonProperty("variables")
    Map<String, Boolean> variables;

}
