package pl.climbbetter.shared.internal.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfiguration {

    @Bean
    OpenAPI climbBetterOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ClimbBetter API")
                .version("v1")
                .description("Training sessions, dashboard and catalog for the ClimbBetter web app."));
    }
}
