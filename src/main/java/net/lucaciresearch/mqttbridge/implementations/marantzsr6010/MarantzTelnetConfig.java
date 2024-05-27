package net.lucaciresearch.mqttbridge.implementations.marantzsr6010;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter @Setter
@Accessors(fluent = true, chain = true)
public class MarantzTelnetConfig {

    @NotNull @Size(min = 1, max = 200)
    String host;



}
