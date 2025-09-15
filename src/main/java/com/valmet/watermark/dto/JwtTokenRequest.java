package com.valmet.watermark.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for user authentication requests.
 */
@Builder
@Getter
@Setter
public class JwtTokenRequest {
    /**
     * The username of the user attempting to authenticate.
     */
    @NotEmpty (message = "Username must not be empty")
    private String username;
    /**
     * The password of the user attempting to authenticate.
     */
    @NotEmpty (message = "Password must not be empty")
    private String password;
}
