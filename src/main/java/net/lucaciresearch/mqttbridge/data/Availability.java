package net.lucaciresearch.mqttbridge.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true, chain = true)
@AllArgsConstructor
public enum Availability {
    AVAILABLE(true), UNAVAILABLE(false);

    private final boolean bool;


}
