package net.lucaciresearch.mqttbridge.util;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import net.lucaciresearch.mqttbridge.mqtt.DiscoveryConfig;
import net.lucaciresearch.mqttbridge.mqtt.MqttConfig;

@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true, chain = true)
@Getter
public class Config<DCTy> {

    @NotNull @Valid
    private MqttConfig mqtt;

    @NotNull @Valid
    private DiscoveryConfig discovery;

    @NotNull @Valid
    private DCTy device;

    private String deviceCodename;



}
