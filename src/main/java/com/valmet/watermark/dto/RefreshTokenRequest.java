package com.valmet.watermark.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Data Transfer Object (DTO) for handling refresh token requests.
 * <p>
 * This class encapsulates the refresh token provided by the client during a
 * token renewal process. It is used as the request payload in APIs that manage
 * token renewal operations.
 * </p>
 *
 * <p>
 * <b>Annotations:</b>
 * </p>
 * <ul>
 * <li>{@link Builder}: Provides a fluent API for creating instances of this
 * class.</li>
 * <li>{@link Getter}: Automatically generates getter methods for all
 * fields.</li>
 * </ul>
 *
 * <p>
 * <b>Usage:</b>
 * </p>
 *
 * <pre>
 * // Example usage of the builder pattern to create an instance:
 * RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("sample-refresh-token").build();
 *
 * // Accessing the refresh token:
 * String token = request.getRefreshToken();
 * </pre>
 *
 * <p>
 * <b>Dependencies:</b>
 * </p>
 * <ul>
 * <li>Lombok: Reduces boilerplate code by generating builder and getter
 * methods.</li>
 * </ul>
 *
 * <p>
 * <b>Example REST API:</b>
 * </p>
 *
 * <pre>
 * {@code
 * &#64;PostMapping("/api/renewToken")
 * public ResponseEntity<?> renewToken(@RequestBody RefreshTokenRequest request) {
 *     String refreshToken = request.getRefreshToken();
 *     // Logic to renew the token
 *     return ResponseEntity.ok(newToken);
 * }
 * }
 * </pre>
 *
 * @author [Your Name]
 */
@Getter
public class RefreshTokenRequest {
    /**
     * The refresh token string provided by the client for token renewal.
     */
    private String refreshToken;
}