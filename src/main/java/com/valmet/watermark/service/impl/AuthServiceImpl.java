package com.valmet.watermark.service.impl;

import com.valmet.watermark.config.JwtSettings;
import com.valmet.watermark.config.security.WatermarkSecretsManager;
import com.valmet.watermark.constants.Constants;
import com.valmet.watermark.dto.JwtTokenRequest;
import com.valmet.watermark.dto.RefreshTokenRequest;
import com.valmet.watermark.dto.SecretDto;
import com.valmet.watermark.enums.ResultCodeConstants;
import com.valmet.watermark.enums.TokenType;
import com.valmet.watermark.exception.WatermarkApplicationException;
import com.valmet.watermark.response.AuthenticationResponseDTO;
import com.valmet.watermark.service.AuthService;
import com.valmet.watermark.service.JwtTokenService;
import com.valmet.watermark.service.LdapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.valmet.watermark.response.WatermarkResponseUtil.throwApplicationException;

/**
 * Implementation of the {@link AuthService} interface responsible for handling
 * authentication and token management.
 * <p>
 * This class provides functionality for:
 * <ul>
 * <li>Authenticating users and generating JWT access and refresh tokens.</li>
 * <li>Validating and renewing refresh tokens.</li>
 * </ul>
 *
 * @author BJIT
 * @version 1.0
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    private final JwtTokenService tokenService;
    private final WatermarkSecretsManager watermarkSecretsManager;
    private final JwtSettings jwtSettings;
    private final LdapService ldapService;
    private final Pattern emailPattern = Pattern.compile ("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * Constructor to initialize dependencies.
     *
     * @param tokenService            the service used for JWT token generation and
     *                                validation
     * @param watermarkSecretsManager the secrets manager for retrieving valid
     *                                credentials
     * @param jwtSettings             the JWT settings, including expiration times
     *                                for tokens
     */
    public AuthServiceImpl (JwtTokenService tokenService, WatermarkSecretsManager watermarkSecretsManager,
                            JwtSettings jwtSettings, LdapService ldapService) {
        this.tokenService = tokenService;
        this.watermarkSecretsManager = watermarkSecretsManager;
        this.jwtSettings = jwtSettings;
        this.ldapService = ldapService;
    }

    /**
     * Authenticates a user based on the provided credentials and generates JWT
     * tokens.
     *
     * @param request the authentication request containing username and password
     * @return an {@link AuthenticationResponseDTO} containing the access token and
     * refresh token
     * @throws IOException                   if an I/O error occurs during
     *                                       authentication
     * @throws WatermarkApplicationException if the username or password is invalid
     */
    @Override
    public AuthenticationResponseDTO authenticate (JwtTokenRequest request) throws IOException {
        log.info ("User name: {}", request.getUsername ());
        SecretDto secretDto = watermarkSecretsManager.getSecretDto ();
        boolean isAuthenticated = false;
        Matcher matcher = emailPattern.matcher (request.getUsername ());
        if (matcher.matches ()) {
            log.info ("Email address provided for authentication");
            isAuthenticated = ldapService.authenticateWithUPN (request.getUsername (), request.getPassword ());
            log.info ("Authentication with UPN result: {}", isAuthenticated);
            if (!isAuthenticated) {
                isAuthenticated = ldapService.authenticateWithEmail (request.getUsername (), request.getPassword ());
                log.info ("Authentication with email result: {}", isAuthenticated);
            }
        } else {
            isAuthenticated = ldapService.authenticateToMicrosoftAD (request.getUsername (), request.getPassword ());
            log.info ("Authentication with Microsoft AD result: {}", isAuthenticated);
        }
        log.info ("isAuthenticated: {}", isAuthenticated);
        if (!isAuthenticated) {
            String userName = secretDto.getData ().get (Constants.WATERMARK_USERNAME);
            String password = secretDto.getData ().get (Constants.WATERMARK_PASSWORD);
            // Validate login credentials
            if (!userName.equals (request.getUsername ()) || !password.equals (request.getPassword ())) {
                throwApplicationException (ResultCodeConstants.WRONG_CREDENTIALS);
            }

        }

        // Create authentication token
        var authenticationToken = new UsernamePasswordAuthenticationToken (request.getUsername (), request.getPassword ());
        Integer intRSAKeyVersion = secretDto.getMetadata ().getVersion ();
        log.info ("intRSAKeyVersion in AuthService impl: {}", intRSAKeyVersion);

        return AuthenticationResponseDTO.builder ()
                .accessToken (tokenService.generateToken (authenticationToken, jwtSettings.getTokenExpirationTime (), jwtSettings.getTokenIssuer (),
                        TokenType.ACCESS, intRSAKeyVersion, Constants.WATERMARK_ADD))
                .refreshToken (tokenService.generateToken (authenticationToken, jwtSettings.getRefreshTokenExpTime (), jwtSettings.getTokenIssuer (),
                        TokenType.REFRESH, intRSAKeyVersion, Constants.TOKEN_RENEW))
                .build ();
    }

    /**
     * Renews the access token using a valid refresh token.
     *
     * @param refreshTokenRequest the request containing the refresh token
     * @return an {@link AuthenticationResponseDTO} containing the new access token
     * and the existing refresh token
     */
    @Override
    public AuthenticationResponseDTO renewToken (RefreshTokenRequest refreshTokenRequest) throws Exception {
        // Validate the refresh token and retrieve authentication
        var authentication = tokenService.validateRefreshToken (refreshTokenRequest.getRefreshToken ());
        // Generate new access token
        return AuthenticationResponseDTO.builder ().accessToken (
                        tokenService.generateToken (authentication, jwtSettings.getTokenExpirationTime (), jwtSettings.getTokenIssuer (),
                                TokenType.ACCESS, watermarkSecretsManager.getSecretDto ().getMetadata ().getVersion (), Constants.WATERMARK_ADD))
                .refreshToken (refreshTokenRequest.getRefreshToken ()).build ();
    }
}