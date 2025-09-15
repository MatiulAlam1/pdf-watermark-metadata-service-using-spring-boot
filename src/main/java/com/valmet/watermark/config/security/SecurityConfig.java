package com.valmet.watermark.config.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.valmet.watermark.config.CustomAuthenticationEntryPoint;
import com.valmet.watermark.config.CustomJwtAuthenticationFilter;
import com.valmet.watermark.constants.Constants;
import com.valmet.watermark.dto.SecretDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Security configuration class for the Spring Boot application.
 * <p>
 * Configures security settings, including authentication mechanisms, password
 * encoding, JWT token management, and session management. It also sets up
 * filters for handling JWT authentication and access control rules.
 * </p>
 *
 * @author BJIT
 * @version 1.0
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] AUTH_WHITELIST = {
            "/actuator/health", "/actuator/metrics", "/actuator/metrics/**", "api/authenticate", "/actuator/refresh", "/actuator/prometheus",
            "api/renewToken", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/api/monitor/**", "/swagger-resources",
            "/swagger-resources/**", "/apidocs/**"};
    private final Map<String, JwtEncoder> jwtEncoderCache = new ConcurrentHashMap<> ();
    private final Map<String, JwtDecoder> jwtDecoderCache = new ConcurrentHashMap<> ();
    private final WatermarkSecretsManager watermarkSecretsManager;


    /**
     * Constructor that injects the {@link WatermarkSecretsManager}.
     *
     * @param watermarkSecretsManager the secrets manager containing user
     *                                credentials.
     */
    public SecurityConfig (WatermarkSecretsManager watermarkSecretsManager) {
        this.watermarkSecretsManager = watermarkSecretsManager;
    }

    /**
     * Configures the security filter chain.
     * <p>
     * Defines access control rules, session management, CSRF protection, and custom
     * filters.
     * </p>
     *
     * @param httpSecurity the {@link HttpSecurity} instance.
     * @return the configured {@link SecurityFilterChain}.
     * @throws Exception if configuration fails.
     */
    @Bean
    public SecurityFilterChain securityFilterChain (HttpSecurity httpSecurity) throws Exception {
        log.info ("Configuring security filter chain");
        return httpSecurity
                .authorizeHttpRequests (auth -> auth
                        .requestMatchers (AUTH_WHITELIST).permitAll ()
                        .requestMatchers ("api/**").authenticated ())
                .csrf (AbstractHttpConfigurer::disable)
                .sessionManagement (session -> session.sessionCreationPolicy (SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer (oauth2 -> oauth2.jwt (withDefaults ())
                        .authenticationEntryPoint (entryPoint ()))
                .httpBasic (withDefaults ()).headers (header -> header.frameOptions (HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .addFilterBefore (customJwtAuthenticationFilter (), UsernamePasswordAuthenticationFilter.class).build ();
    }

    /**
     * Provides a custom entry point for unauthorized requests.
     *
     * @return a {@link CustomAuthenticationEntryPoint} instance.
     */
    @Bean
    public CustomAuthenticationEntryPoint entryPoint () {
        log.info ("Creating CustomAuthenticationEntryPoint bean");
        return new CustomAuthenticationEntryPoint ();
    }

    /**
     * Creates an instance of the custom JWT authentication filter.
     *
     * @return a {@link CustomJwtAuthenticationFilter}.
     */
    @Bean
    public CustomJwtAuthenticationFilter customJwtAuthenticationFilter () {
        log.info ("Creating CustomJwtAuthenticationFilter bean");
        return new CustomJwtAuthenticationFilter ();
    }

    /**
     * Configures the authentication manager.
     *
     * @param userDetailsService the {@link UserDetailsService} for user
     *                           authentication.
     * @return the {@link AuthenticationManager}.
     */
    @Bean
    public AuthenticationManager authenticationManager (UserDetailsService userDetailsService) {
        log.info ("Creating AuthenticationManager bean");
        var authenticationProvider = new DaoAuthenticationProvider ();
        authenticationProvider.setUserDetailsService (userDetailsService);
        return new ProviderManager (authenticationProvider);
    }

    /**
     * Configures an in-memory user details service with credentials from the
     * secrets manager.
     *
     * @return an {@link UserDetailsService} instance.
     * @throws IOException if user details cannot be fetched.
     */
    @Bean
    public UserDetailsService userDetailsService () throws IOException {
        log.info ("Creating UserDetailsService bean");
        SecretDto secretDto = watermarkSecretsManager.getSecretDto ();
        UserDetails user2 = User.withUsername (secretDto.getData ().get (Constants.WATERMARK_USERNAME).trim ())
                .password (passwordEncoder ().encode (secretDto.getData ().get (Constants.WATERMARK_PASSWORD).trim ()))
                .authorities ("ROLE_ADMIN").roles ("ADMIN").build ();
        return new InMemoryUserDetailsManager (user2);
    }

    /**
     * Configures a password encoder for encoding user passwords.
     *
     * @return a {@link PasswordEncoder}.
     */
    @Bean
    public PasswordEncoder passwordEncoder () {
        log.info ("Creating PasswordEncoder bean");
        return new BCryptPasswordEncoder ();
    }

    /**
     * Configures a JWT encoder using the RSA private key.
     *
     * @return a {@link JwtEncoder}.
     * @throws Exception if encoding fails.
     */
    @Bean
    @Scope (value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public JwtEncoder jwtEncoder () throws Exception {
        log.info ("Inside jwtEncoder in Security Config");
        SecretDto secretDto = watermarkSecretsManager.getSecretDto ();
        String privateKeyEncoded = Base64.getEncoder ().encodeToString (readPrivateKey (secretDto).getEncoded ());

        return jwtEncoderCache.computeIfAbsent (privateKeyEncoded, key -> {
            try {
                log.info ("Inside jwtEncoder value put to map in Security Config");
                RSAKey rsaKey = new RSAKey.Builder (readPublicKey (secretDto))
                        .privateKey (readPrivateKey (secretDto))
                        .keyID (UUID.randomUUID ().toString ())
                        .build ();
                JWKSet jwkSet = new JWKSet (rsaKey);
                JWKSource<SecurityContext> jwkSource = (jwkSelector, securityContext) -> jwkSelector.select (jwkSet);
                return new NimbusJwtEncoder (jwkSource);
            } catch (Exception e) {
                throw new IllegalStateException (e);
            }
        });
    }

    /**
     * Reads the RSA private key.
     *
     * @param secretDto the {@link SecretDto} containing the private key.
     * @return the {@link PrivateKey}.
     * @throws Exception if the private key cannot be read.
     */
    private PrivateKey readPrivateKey (SecretDto secretDto) throws Exception {
        log.info ("Reading private key from SecretDto");
        String key = secretDto.getData ().get (Constants.PRIVATE_KEY);
        if (key == null) {
            log.error ("Private key not found in vault");
            throw new Exception ("Private key not found in vault");
        }
        byte[] keyBytes = java.util.Base64.getDecoder ().decode (key.replaceAll ("\\s", ""));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec (keyBytes);
        KeyFactory kf = KeyFactory.getInstance ("RSA");
        return kf.generatePrivate (spec);
    }

    /**
     * Reads the RSA public key.
     *
     * @param secretDto the {@link SecretDto} containing the public key.
     * @return the {@link RSAPublicKey}.
     * @throws Exception if the public key cannot be read.
     */
    private RSAPublicKey readPublicKey (SecretDto secretDto) throws Exception {
        log.info ("Reading public key from SecretDto");
        String key = secretDto.getData ().get (Constants.PUBLIC_KEY);
        if (key == null) {
            log.error ("Public key not found in vault");
            throw new Exception ("Public key not found in vault");
        }
        return watermarkSecretsManager.readPublicKey (key);
    }

    /**
     * Configures a JWT decoder using the RSA public key.
     *
     * @return a {@link JwtDecoder}.
     * @throws JOSEException if decoding fails.
     */
    @Bean
    @Scope (value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public JwtDecoder jwtDecoder () throws Exception {
        log.info ("Creating JwtDecoder bean");
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes ();
        if (attributes == null) {
            log.info ("No token found in request");
            return NimbusJwtDecoder.withPublicKey (readPublicKey (watermarkSecretsManager.getSecretDto ())).build ();
        }

        HttpServletRequest request = attributes.getRequest ();
        String requestId = (String) request.getAttribute (Constants.REQUEST_ID);
        log.info ("Request ID in Security Config: {}", requestId);
        Integer intRsaKeyVersion = CustomJwtAuthenticationFilter.getMapVersion ().get (requestId);
        log.info ("RSA key version in Security Config: {}", intRsaKeyVersion);
        if (intRsaKeyVersion != null) {
            RSAPublicKey rsaPublicKey = CustomJwtAuthenticationFilter.getMapRsaPublicKey ().get (intRsaKeyVersion);
            if (rsaPublicKey != null) {
                String publicKeyEncoded = Base64.getEncoder ().encodeToString (rsaPublicKey.getEncoded ());
                return jwtDecoderCache.computeIfAbsent (publicKeyEncoded, key -> {
                    try {
                        log.info ("Creating new JwtDecoder for public key");
                        return NimbusJwtDecoder.withPublicKey (rsaPublicKey).build ();
                    } catch (Exception e) {
                        log.error ("Error creating JwtDecoder", e);
                        throw new IllegalStateException (e);
                    }
                });
            }
        }
        return NimbusJwtDecoder.withPublicKey (readPublicKey (watermarkSecretsManager.getSecretDto ())).build ();
    }
}