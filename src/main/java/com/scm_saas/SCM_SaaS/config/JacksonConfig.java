package com.scm_saas.SCM_SaaS.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.scm_saas.SCM_SaaS.util.DynamicEntitySerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Configure date format
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        mapper.setDateFormat(dateFormat);

        // Disable timestamp output for dates
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Register custom serializer for dynamic entities
        SimpleModule module = new SimpleModule();
        module.addSerializer(Object.class, new DynamicEntitySerializer());
        mapper.registerModule(module);

        return mapper;
    }

}
