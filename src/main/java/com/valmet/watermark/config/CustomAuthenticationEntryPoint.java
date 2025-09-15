package com.valmet.watermark.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom implementation of the {@link AuthenticationEntryPoint} to handle
 * authentication failures.
 * <p>
 * This class customizes the response when an authentication failure occurs,
 * such as when a JWT token is expired or invalid. It also logs the error for
 * debugging purposes. The class delegates default behavior to the
 * {@link BearerTokenAuthenticationEntryPoint} while adding additional logic for
 * handling JWT-specific errors.
 * </p>
 *
 * <p>
 * <b>Key Features:</b>
 * </p>
 * <ul>
 * <li>Intercepts authentication errors and returns appropriate error messages
 * in JSON format.</li>
 * <li>Logs detailed error information using SLF4J.</li>
 * <li>Delegates basic handling to {@link BearerTokenAuthenticationEntryPoint}
 * for standard OAuth2 behavior.</li>
 * </ul>
 *
 * <p>
 * The class is annotated with:
 * </p>
 * <ul>
 * <li>{@link Component}: Marks this class as a Spring-managed bean for
 * dependency injection.</li>
 * <li>{@link Slf4j}: Provides logging capabilities for error tracking and
 * debugging.</li>
 * </ul>
 *
 * <p>
 * <b>Usage:</b>
 * </p>
 * <ul>
 * <li>Spring Security automatically uses this class when configured as the
 * authentication entry point.</li>
 * <li>Handles exceptions such as expired JWT tokens or other authentication
 * failures.</li>
 * </ul>
 *
 * <p>
 * <b>Behavior:</b>
 * </p>
 * <ul>
 * <li>Returns a 401 Unauthorized HTTP status for authentication failures.</li>
 * <li>Responds with JSON error messages for specific scenarios:
 * <ul>
 * <li>If the JWT token is expired: {@code {"error": "Token has expired",
 * "code": "500"}}</li>
 * <li>For all other failures: {@code {"error": "Authentication
 * failed!!!"}}</li>
 * </ul>
 * </li>
 * </ul>
 *
 * @author BJIT
 * @version 1.0
 */
@Component
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    /**
     * The delegate {@link BearerTokenAuthenticationEntryPoint} to handle default
     * behavior.
     */
    private final AuthenticationEntryPoint delegate = new BearerTokenAuthenticationEntryPoint ();

    /**
     * Default constructor.
     */
    public CustomAuthenticationEntryPoint () {
    }

    /**
     * Handles authentication failures and customizes the error response.
     * <p>
     * This method is triggered whenever an authentication exception is thrown. It
     * returns an appropriate JSON response based on the exception message and logs
     * the error for debugging.
     * </p>
     *
     * @param request       the {@link HttpServletRequest} object
     * @param response      the {@link HttpServletResponse} object
     * @param authException the {@link AuthenticationException} that caused the
     *                      failure
     * @throws IOException      if an input or output error occurs while writing the
     *                          response
     * @throws ServletException if an error occurs during request processing
     */
    @Override
    public void commence (HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException, ServletException {
        // Delegate default behavior to BearerTokenAuthenticationEntryPoint
        this.delegate.commence (request, response, authException);
        // Retrieve the error message from the exception
        String errMsg = authException.getMessage ();
        // Write a custom JSON response for specific errors
        if (errMsg.contains ("Jwt expired")) {
            response.getWriter ().write ("{\"error\": \"Token has expired\", \"code\":\"500\"}");
        } else {
            response.getWriter ().write ("{\"error\": \"Authentication failed!!!\"}");
        }
        // Log the error message for debugging purposes
        log.error ("Error in AuthenticationEntryPoint::{}", errMsg);
        // Set the HTTP response status and content type
        response.setStatus (HttpServletResponse.SC_UNAUTHORIZED); // HTTP Status 401 Unauthorized
        response.setContentType ("application/json");
    }
}
