package com.hospital.noshow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI noshowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hospital Appointment No-Show Risk Engine API")
                        .version("1.0")
                        .description("Spring Boot + ML-driven no-show risk scoring for hospital appointments. "
                                + "Most endpoints are session-authenticated via the Thymeleaf login flow; "
                                + "this Bearer scheme exists for direct API/Swagger-driven calls per JwtAuthFilter's "
                                + "session-then-header fallback."))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}