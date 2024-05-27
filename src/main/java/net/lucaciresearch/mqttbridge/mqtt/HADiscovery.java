package net.lucaciresearch.mqttbridge.mqtt;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@AllArgsConstructor
@Data
@Accessors(fluent = true, chain = true)
public class HADiscovery {

    @JsonProperty("availability")
    private Availability availability;

    @JsonProperty("command_topic")
    private String commandTopic;

    @JsonProperty("device")
    private Device device;

    @JsonProperty("name")
    private String varibleName;

    @JsonProperty("origin")
    private Origin origin;

    @JsonProperty("state_topic")
    private String stateTopic;

    @JsonProperty("unique_id")
    private String uniqueId;

    @JsonProperty("value_template")
    private String valueTemplate;





    @AllArgsConstructor
    @Data @Accessors(fluent = true, chain = true)
    public static class Availability {

        @JsonProperty("topic")
        private String topic;

        @JsonProperty("value_template")
        private String valueTemplate;

    }

    @AllArgsConstructor
    @Data @Accessors(fluent = true, chain = true)
    public static class Device {

        @JsonProperty("identifiers")
        private List<String> identifiers;

        @JsonProperty("manufacturer")
        private String manufacturer;

        @JsonProperty("model")
        private String model;

        @JsonProperty("name")
        private String name;

        @JsonProperty("sw_version")
        private String swVersion;

    }

    @AllArgsConstructor
    @Data @Accessors(fluent = true, chain = true)
    public static class Origin {

        @JsonProperty("name")
        private String name;

        @JsonProperty("sw")
        private String sw;

        @JsonProperty("url")
        private String url;
    }


}
