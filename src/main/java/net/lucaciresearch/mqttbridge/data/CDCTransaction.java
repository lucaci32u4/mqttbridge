package net.lucaciresearch.mqttbridge.data;

import lombok.*;
import lombok.experimental.Accessors;
import net.lucaciresearch.mqttbridge.util.TruthSource;

@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true, chain = true)
@ToString
@EqualsAndHashCode
@Getter @Setter
public class CDCTransaction<Ty> {

    private TruthSource source;

    private long timestamp;

    private Ty value;

}
