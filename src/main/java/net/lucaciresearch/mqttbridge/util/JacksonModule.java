package net.lucaciresearch.mqttbridge.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.inject.AbstractModule;

public class JacksonModule extends AbstractModule {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
