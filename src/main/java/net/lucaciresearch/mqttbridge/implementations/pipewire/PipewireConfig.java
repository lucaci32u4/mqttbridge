package net.lucaciresearch.mqttbridge.implementations.pipewire;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter @Setter
@Accessors(fluent = true, chain = true)
public class PipewireConfig {

    @NotNull @Size(min = 1, max = 200)
    String host;

    @NotNull
    private List<FilterChain> filterChains;

    @Getter @Setter
    @Accessors(fluent = true, chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterChain {

        @NotNull @Size(min = 1, max = 200)
        String description;

        @NotNull @Size(min = 1, max = 200)
        String subtopic;

        @NotEmpty
        List<String> plugins;

    }

}
