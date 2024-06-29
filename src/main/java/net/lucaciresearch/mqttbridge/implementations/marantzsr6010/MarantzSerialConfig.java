package net.lucaciresearch.mqttbridge.implementations.marantzsr6010;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Range;

@Getter @Setter
@Accessors(fluent = true, chain = true)
public class MarantzSerialConfig {

    @NotNull @Size(min = 1, max = 200)
    String port;

    @NotNull @Range(min = 1, max = 2000000)
    int baud;



}
