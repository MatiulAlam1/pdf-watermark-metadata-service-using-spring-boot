package com.valmet.watermark.enums;

/**
 * Enum representing the possible types of responses in the application.
 * <p>
 * This enum is used to categorize API responses into two types: {@link #RESULT}
 * and {@link #ERROR}. These values help to standardize the response structure
 * and make it easier to handle different response types in a consistent manner
 * across the application.
 * </p>
 *
 * @author BJIT
 * @version 1.0
 */
public enum ResponseType {
    /**
     * Indicates a successful result or operation.
     */
    RESULT,
    /**
     * Indicates an error or failure in the operation.
     */
    ERROR
}
