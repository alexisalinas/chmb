package com.banamex.cops.update_offer.config;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/** Registra el {@code JsonMapper} con la misma configuración que el {@code ObjectMapper} de Spring Boot. */
@Configuration
public class JsonMapperConfig {

    /** Es también el {@code ObjectMapper} de la aplicación, configurado con el builder de Spring Boot. */
    @Bean
    @Primary
    public JsonMapper jsonMapper(Jackson2ObjectMapperBuilder builder) {
        JsonMapper mapper = JsonMapper.builder().build();
        builder.configure(mapper);
        return mapper;
    }
}
