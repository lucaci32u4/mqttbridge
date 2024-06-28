package net.lucaciresearch.mqttbridge.util;

import com.fasterxml.jackson.databind.*;
import com.google.inject.AbstractModule;

public class JacksonModule extends AbstractModule {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    @Override
    protected void configure() {
        bind(ObjectMapper.class)
                .toInstance(mapper);
        bind(ObjectReader.class)
                .toProvider(mapper::reader);
        bind(ObjectWriter.class)
                .toProvider(mapper::writer);
    }
}
