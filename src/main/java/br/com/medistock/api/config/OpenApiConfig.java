package br.com.medistock.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    private static final String ESQUEMA_BEARER = "bearerAuth";

    @Bean
    public OpenAPI medistockOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MediStock API")
                        .version("1.0")
                        .description("""
                                API REST de gestão de estoque hospitalar.

                                Para usar os endpoints protegidos:
                                1. Cadastre-se em POST /api/v1/auth/registro com e-mail de domínio institucional
                                2. Autentique-se em POST /api/v1/auth/login e copie o campo "token"
                                3. Clique em Authorize, no topo desta página, e cole o token""")
                        .contact(new Contact().name("MediStock")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER))
                .components(new Components().addSecuritySchemes(ESQUEMA_BEARER,
                        new SecurityScheme()
                                .name(ESQUEMA_BEARER)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT devolvido por POST /api/v1/auth/login")));
    }
}
