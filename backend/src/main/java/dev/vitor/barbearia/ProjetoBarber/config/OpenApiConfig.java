package dev.vitor.barbearia.ProjetoBarber.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tenorio's Barber — API de Gestão & Agendamentos")
                        .version("v1.0.0")
                        .description("API RESTful de gestão operacional e financeira de barbearia: agendamentos com prevenção de conflitos (Lock Pessimista), "
                                + "RBAC (Cliente, Barbeiro, Gestor), caixa por período, histórico imutável de atendimentos, cálculo de comissões e proteção Rate Limiting.")
                        .contact(new Contact()
                                .name("Tenorio's Barber Dev Team")
                                .email("gestor@tenoriosbarber.com"))
                        .license(new License()
                                .name("Proprietary / Internal Use")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Insira o token JWT retornado em /api/auth/login ou /api/auth/registrar")));
    }
}
