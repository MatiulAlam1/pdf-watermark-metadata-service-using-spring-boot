package com.valmet.watermark.response;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object (DTO) representing the response for authentication
 * requests.
 * <p>
 * This class is used to encapsulate the authentication tokens (access token and
 * refresh token) returned to the client after a successful login or token
 * renewal operation.
 * </p>
 *
 * @author BJIT
 * @version 1.0
 */
@Data
@Builder
public class AuthenticationResponseDTO {
    /**
     * The JWT access token issued to the client.
     * <p>
     * This token is used to authenticate subsequent API requests. Example:
     * {@code "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."}
     * </p>
     */
    private String accessToken;
    /**
     * The JWT refresh token issued to the client.
     * <p>
     * This token is used to obtain a new access token when the current one expires.
     * Example: {@code "dXNlcm5hbWU6cGFzc3dvcmQ..."}
     * </p>
     */
    private String refreshToken;
}
