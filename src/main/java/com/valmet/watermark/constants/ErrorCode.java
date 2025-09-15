package com.valmet.watermark.constants;

/**
 * A utility class that defines error codes used throughout the application.
 * <p>
 * This class provides a set of static constants representing specific error
 * codes for various error scenarios. These error codes are used to standardize
 * error reporting and facilitate easier debugging and client-side error
 * handling.
 * </p>
 *
 * @author BJIT
 * @version 1.0
 */
public final class ErrorCode {
    public static final String WRONG_CREDENTIALS = "40101";
    public static final String JWT_TOKEN_EXPIRED = "40103";
    public static final String INVALID_JWT_TOKEN = "40104";
    public static final String FILE_REQUIRED = "40105";
    public static final String UNSUPPORTED_FILE_TYPE = "40106";
    public static final String FILE_SIZE_LIMIT = "40107";
    public static final String INVALID_PROPERTY_KEY = "40108";
    public static final String ACCESS_DENIED = "40109";
    public static final String INTERNAL_SERVER_ERROR = "5000";
    public static final int MAINTENANCE_MODE = 9999;

    private ErrorCode () {
    }

}
