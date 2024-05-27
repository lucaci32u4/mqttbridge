package net.lucaciresearch.mqttbridge.mqtt;

import lombok.Data;
import lombok.experimental.Accessors;


@Data
@Accessors(fluent = true, chain = true)
public class TopicMessage {

    private final String topic;

    private final String message;

}
