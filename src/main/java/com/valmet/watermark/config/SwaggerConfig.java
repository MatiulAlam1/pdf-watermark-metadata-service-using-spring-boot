package com.valmet.watermark.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger configuration class.
 * <p>
 * Configures OpenAPI settings, including application title, version, and
 * security schemes.
 * </p>
 *
 * @author BJIT
 * @version 1.0
 */
@Configuration
public class SwaggerConfig {
    /**
     * The currently active Spring profile.
     * <p>
     * Injected from the {@code spring.profiles.active} property to dynamically set
     * environment-specific information in the API documentation.
     * </p>
     */
    @Value ("${spring.profiles.active}")
    private String activeProfile;

    /**
     * Configures the OpenAPI definition for the application.
     * <p>
     * This method sets up API metadata, security schemes, and global security
     * requirements.
     * </p>
     *
     * @return an {@link OpenAPI} instance with the customized configuration.
     */
    @Bean
    public OpenAPI customOpenAPI () {
        // Define the security scheme
        SecurityScheme securityScheme = new SecurityScheme ().type (SecurityScheme.Type.HTTP).scheme ("bearer")
                .bearerFormat ("JWT").description ("JWT Bearer token authentication");

        // Add the security scheme to components
        Components components = new Components ().addSecuritySchemes ("Bearer Authentication", securityScheme);

        // Add global security requirement
        SecurityRequirement securityRequirement = new SecurityRequirement ().addList ("Bearer Authentication");

        // Return the OpenAPI definition
        return new OpenAPI ().components (components)
                // .addSecurityItem(securityRequirement) // Apply globally
                .info (new Info ().title ("Watermark API Documentation in " + activeProfile).version ("1.0")
                        .description ("API documentation with custom security requirements"));
    }
}
