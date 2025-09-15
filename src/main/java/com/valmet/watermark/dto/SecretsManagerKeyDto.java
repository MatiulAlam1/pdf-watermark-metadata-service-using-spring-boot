package com.valmet.watermark.dto;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * Data Transfer Object (DTO) for representing secrets managed by a secret manager.
 * <p>
 * This class encapsulates the username and password retrieved from a secret management service
 * and provides validation methods to ensure the fields are populated.
 * </p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Stores credentials such as {@code username} and {@code password}.</li>
 *   <li>Provides a static method for validating the DTO's fields.</li>
 *   <li>Leverages Lombok's {@link Data} annotation to generate boilerplate code, such as getters, setters, and `toString`.</li>
 * </ul>
 *
 * <p><b>Validation:</b></p>
 * <ul>
 *   <li>The {@link #validate(SecretsManagerKeyDto)} method ensures that both the {@code username} and {@code password} are not blank.</li>
 *   <li>Throws a {@link RuntimeException} if any of the fields are blank, specifying the problematic field name.</li>
 * </ul>
 *
 * <p><b>Usage:</b></p>
 * <ol>
 *   <li>Populate this DTO with credentials retrieved from a secret management service.</li>
 *   <li>Call the {@link #validate(SecretsManagerKeyDto)} method to ensure the fields are valid before further processing.</li>
 * </ol>
 *
 * <p><b>Example:</b></p>
 * <pre>
 * SecretsManagerKeyDto dto = new SecretsManagerKeyDto();
 * dto.setUsername("myUsername");
 * dto.setPassword("myPassword");
 * SecretsManagerKeyDto.validate(dto);
 * </pre>
 *
 * <p><b>Dependencies:</b></p>
 * <ul>
 *   <li>Lombok for generating boilerplate code.</li>
 *   <li>Apache Commons Lang for string utility methods.</li>
 * </ul>
 *
 * @author BJIT
 */
@Data
public class SecretsManagerKeyDto {
    /**
     * The username retrieved from the secret management service.
     */
    private String username;
    /**
     * The password retrieved from the secret management service.
     */
    private String password;

    /**
     * Validates that the {@code username} and {@code password} fields are not blank.
     * <p>
     * This method ensures that the fields are populated with non-blank values. If either field is blank,
     * a {@link RuntimeException} is thrown with an appropriate error message.
     * </p>
     *
     * @param secretsManagerKeyDto the {@link SecretsManagerKeyDto} instance to validate
     * @throws RuntimeException if any field is blank
     */
    public static void validate (SecretsManagerKeyDto secretsManagerKeyDto) {
        throwIfBlank (secretsManagerKeyDto.getUsername (), "username");
        throwIfBlank (secretsManagerKeyDto.getPassword (), "password");
    }

    /**
     * Checks if a string is blank and throws an exception if it is.
     *
     * @param str     the string to check
     * @param keyName the name of the field being checked, used in the exception message
     * @throws RuntimeException if the string is blank
     */
    private static void throwIfBlank (String str, String keyName) {
        if (StringUtils.isBlank (str)) {
            throw new RuntimeException (keyName + ": is blank in " + SecretsManagerKeyDto.class.getName ());
        }
    }
}
