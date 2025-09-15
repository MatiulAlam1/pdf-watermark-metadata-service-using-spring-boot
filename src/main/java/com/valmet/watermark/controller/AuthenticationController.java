package com.valmet.watermark.controller;

import com.valmet.watermark.constants.Constants;
import com.valmet.watermark.dto.JwtTokenRequest;
import com.valmet.watermark.dto.RefreshTokenRequest;
import com.valmet.watermark.enums.ResponseType;
import com.valmet.watermark.response.BaseResponse;
import com.valmet.watermark.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;

/**
 * AuthenticationController for handling authentication and token generation.
 * <p>
 * This controller provides endpoints for user authentication using OAuth2 and
 * JWT. After successful authentication, JWT access and refresh tokens are
 * returned to the client.
 * </p>
 *
 * @author BJIT
 * @version 1.0
 */
@RestController
@RequestMapping ("/api")
@Validated
@Tag (name = "Authentication API")
public class AuthenticationController {
    private final AuthService authService;

    /**
     * Constructor to inject AuthService dependency.
     *
     * @param authService the service responsible for token generation and
     *                    validation.
     */
    public AuthenticationController (AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticates the user and generates JWT access token and refresh token.
     *
     * @param authRequest the authentication request containing username and
     *                    password.
     * @return a response containing the generated JWT access and refresh tokens.
     * @throws IOException if an error occurs during authentication.
     */
    @PostMapping ("/authenticate")
    @Operation (security = {}, summary = "User Authentication", description = "Authenticate user and return access & refresh tokens.")
    @ApiResponses (value = {
            @ApiResponse (responseCode = "200", description = "Successfully authenticated. Returns JWT access and refresh tokens.", content = @Content (mediaType = "application/json", schema = @Schema (implementation = BaseResponse.class))),
            @ApiResponse (responseCode = "401", description = "Invalid credentials. Authentication failed.", content = @Content (mediaType = "application/json")),
            @ApiResponse (responseCode = "500", description = "Internal server error while processing the login request.", content = @Content (mediaType = "application/json"))})
    public BaseResponse authenticate (@Valid @RequestBody JwtTokenRequest authRequest) throws IOException {
        return BaseResponse.builder ()
                .responseType (ResponseType.RESULT)
                .message (Collections.singleton (HttpStatus.OK.getReasonPhrase ()))
                .result (authService.authenticate (authRequest))
                .code (Constants.SUCCESS_CODE)
                .build ();
    }

    /**
     * Endpoint to refresh the JWT token.
     *
     * @param refreshTokenRequest that contains JWT refresh token.
     * @return a response containing a new access token and same refresh token.
     * @throws IOException if an error occurs during authentication.
     */
    @PostMapping ("/renewToken")
    @Operation (security = {}, summary = "Refresh Access Token", description = "Use the refresh token to generate a new access token.")
    @ApiResponses (value = {
            @ApiResponse (responseCode = "200", description = "Successfully generated a new access token.", content = @Content (mediaType = "application/json", schema = @Schema (implementation = BaseResponse.class))),
            @ApiResponse (responseCode = "401", description = "Invalid or expired refresh token.", content = @Content (mediaType = "application/json")),
            @ApiResponse (responseCode = "500", description = "Internal server error while processing the token renewal request.", content = @Content (mediaType = "application/json"))})
    public BaseResponse renewToken (@RequestBody RefreshTokenRequest refreshTokenRequest) throws Exception {
        return BaseResponse.builder ()
                .responseType (ResponseType.RESULT)
                .message (Collections.singleton (HttpStatus.OK.getReasonPhrase ()))
                .result (authService.renewToken (refreshTokenRequest))
                .code (Constants.SUCCESS_CODE)
                .build ();
    }
}