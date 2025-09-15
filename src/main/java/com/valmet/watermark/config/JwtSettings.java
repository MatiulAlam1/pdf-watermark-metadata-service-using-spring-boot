package com.valmet.watermark.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Configuration class for JWT (JSON Web Token) settings.
 * <p>
 * This class maps properties from the `security-info.properties` file into
 * fields that can be used throughout the application to configure JWT behavior.
 * The properties are prefixed with `security.jwt`.
 * </p>
 *
 * <p>
 * <b>Usage:</b>
 * </p>
 * <ul>
 * <li>Ensure the `security-info.properties` file exists in the classpath.</li>
 * <li>Define JWT-related properties in the file using the `security.jwt`
 * prefix.</li>
 * <li>This class is automatically loaded as a Spring bean for easy
 * injection.</li>
 * </ul>
 *
 * <p>
 * <b>Example Properties in `security-info.properties`:</b>
 * </p>
 *
 * <pre>
 * security.jwt.tokenExpirationTime=3600
 * security.jwt.tokenIssuer=MyAppIssuer
 * security.jwt.refreshTokenExpTime=7200
 * security.jwt.header=Authorization
 * security.jwt.prefix=Bearer
 * </pre>
 *
 * <p>
 * <b>Fields:</b>
 * </p>
 * <ul>
 * <li>{@code tokenExpirationTime}: Duration (in seconds) for which the JWT
 * token is valid.</li>
 * <li>{@code tokenIssuer}: The issuer of the JWT token.</li>
 * <li>{@code refreshTokenExpTime}: Duration (in seconds) for which the refresh
 * token is valid.</li>
 * <li>{@code header}: The HTTP header used to pass the JWT token.</li>
 * <li>{@code prefix}: Prefix (e.g., "Bearer") added to the token in the HTTP
 * header.</li>
 * </ul>
 *
 * <p>
 * This class is annotated with:
 * </p>
 * <ul>
 * <li>{@link Configuration}: Marks the class as a source of bean
 * definitions.</li>
 * <li>{@link PropertySource}: Specifies the properties file to load
 * (`security-info.properties`).</li>
 * <li>{@link ConfigurationProperties}: Maps properties with the prefix
 * `security.jwt` to the fields of this class.</li>
 * <li>{@link Data}: Lombok annotation to generate getters, setters, and other
 * utility methods.</li>
 * </ul>
 *
 * @author BJIT
 * @version 1.0
 */
@Configuration
@PropertySource ("classpath:security-info.properties")
@ConfigurationProperties (prefix = "security.jwt")
@Data
public class JwtSettings {
    /**
     * The duration (in seconds) for which the JWT token is valid.
     */
    private Integer tokenExpirationTime;
    /**
     * The issuer of the JWT token. This typically identifies the application or
     * service that generates the token.
     */
    private String tokenIssuer;
    /**
     * The duration (in seconds) for which the refresh token is valid.
     */
    private Integer refreshTokenExpTime;
    private String header;
    private String prefix;
}
