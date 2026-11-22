package pl.climbbetter.shared.internal.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfiguration {

    @Bean
    OpenAPI climbBetterOpenApi() {
        return new OpenAPI().components(new Components().addSecuritySchemes("firebase",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("Firebase ID token")))
                .addSecurityItem(new SecurityRequirement().addList("firebase")).info(new Info()
                .title("ClimbBetter API")
                .version("v1")
                .description("Training sessions, dashboard and catalog for the ClimbBetter web app."));
    }
}
