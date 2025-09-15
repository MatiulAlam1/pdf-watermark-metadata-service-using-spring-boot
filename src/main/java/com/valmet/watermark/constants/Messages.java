package com.valmet.watermark.constants;

import lombok.Data;

/**
 * A utility class that contains predefined messages used throughout the
 * application.
 * <p>
 * This class defines constant message strings for common error scenarios, such
 * as bad requests and internal server errors. These messages can be reused to
 * ensure consistency in error handling and response generation.
 * </p>
 *
 * @author BJIT
 * @version 1.0
 */
@Data
public class Messages {
    public static final String BAD_REQUEST = "Bad Request";
    public static final String SERVER_ERROR = "Internal Server Error";
}
