package com.joaoandrade.todoapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI secureTaskApiInfo() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(
                        "basicAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")
                ))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
                .info(new Info()
                        .title("Secure Task API")
                        .version("v1")
                        .description("API REST segura para gestao de tarefas, construida com Spring Boot."));
    }
}
