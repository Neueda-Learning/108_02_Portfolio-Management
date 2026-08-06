package com.example.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {
    
    @Bean
    public OpenAPI portfolioManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Portfolio Management API")
                        .description("REST API for managing financial portfolios including stocks, bonds, and other assets")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Portfolio Management Team - Group 2")
                                .email("support@portfoliomanagement.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}

