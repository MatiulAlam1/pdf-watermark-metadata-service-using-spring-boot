package com.valmet.watermark.enums;

/**
 * Enum representing various result codes used throughout the application.
 * <p>
 * This enum defines a set of constant values representing different types of
 * errors or result codes that may occur during the execution of the
 * application. These codes can be used in response objects, logging, and
 * exception handling to provide consistent and meaningful error reporting.
 * </p>
 *
 * @author BJIT
 * @version 1.0
 */
public enum ResultCodeConstants {
    INTERNAL_SERVER_ERROR, FILE_SIZE_LIMIT, FILE_REQUIRED, UNSUPPORTED_FILE_TYPE, WRONG_CREDENTIALS, TOKEN_EXPIRED, INVALID_PROPERTY_KEY,
    ResultCodeConstants () {
    }
}
