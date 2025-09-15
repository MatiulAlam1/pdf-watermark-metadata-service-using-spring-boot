package com.valmet.watermark.service;

import com.valmet.watermark.dto.JwtTokenRequest;
import com.valmet.watermark.dto.RefreshTokenRequest;
import com.valmet.watermark.response.AuthenticationResponseDTO;

import java.io.IOException;

/**
 * AuthService interface for handling authentication and token management operations.
 * <p>
 * This interface defines methods for:
 * <ul>
 *     <li>Authenticating users and generating JWT access and refresh tokens.</li>
 *     <li>Renewing access tokens using a valid refresh token.</li>
 * </ul>
 */
public interface AuthService {
    AuthenticationResponseDTO authenticate (JwtTokenRequest request) throws IOException;

    AuthenticationResponseDTO renewToken (RefreshTokenRequest refreshTokenRequest) throws Exception;

}
