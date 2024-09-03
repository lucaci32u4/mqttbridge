package net.lucaciresearch.mqttbridge.data;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.lucaciresearch.mqttbridge.util.TruthSource;

@Getter @Setter
@Accessors(chain = true, fluent = true)
public class CDCTransaction<Ty, Dty> {

    private TruthSource source;

    private String mqttValue;

    private Ty nativeValue;

    private Dty deviceValue;

}
