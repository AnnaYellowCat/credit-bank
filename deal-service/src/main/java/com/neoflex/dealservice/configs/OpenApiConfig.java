package com.neoflex.dealservice.configs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class OpenApiConfig {
    private static final String OPENAPI_FILENAME = "openapi.yaml";

    @Bean
    public OpenAPI openAPI() throws IOException {
        try (InputStream inputStream = new ClassPathResource(OPENAPI_FILENAME).getInputStream()) {
            String spec = new String(inputStream.readAllBytes());
            return new OpenAPIV3Parser().readContents(spec).getOpenAPI();
        }
    }
}