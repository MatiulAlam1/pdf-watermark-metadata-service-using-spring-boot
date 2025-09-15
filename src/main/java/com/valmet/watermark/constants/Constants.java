package com.valmet.watermark.constants;

import java.text.SimpleDateFormat;

/**
 * A utility class that contains constant values used throughout the
 * application.
 * <p>
 * This class defines a set of static constants for common values, such as
 * header names, status codes, token types, cloud providers, and date formatting
 * patterns.
 * </p>
 *
 * @author BJIT
 * @version 1.0
 */
public class Constants {
    public static final String AUTHENTICATION_HEADER_NAME = "Authorization";
    public static final String SUCCESS_CODE = "200";
    public static final String TOKEN_TYPE = "token_type";
    public static final String RSA_KEY_VERSION = "ras_key_version";
    public static final String AWS = "AWS";
    public static final String AZURE = "AZURE";
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    public static final String UNDER_SCORE = "_";
    public static final String DATA = "data";
    public static final String REQUEST_ID = "requestId";
    public static final String PRIVATE_KEY = "private_key";
    public static final String PUBLIC_KEY = "public_key";
    public static final String VERSION = "version";
    public static final String WATERMARK_USERNAME = "watermark.username";
    public static final String WATERMARK_PASSWORD = "watermark.password";
    public static final String WATERMARK_ADD = "watermark:add";
    public static final String TOKEN_RENEW = "token:renew";
    public static final String PERSON_ID = "employeeID";

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private Constants() {
    }
}
