package com.valmet.watermark.response;

import com.valmet.watermark.constants.ErrorCode;
import com.valmet.watermark.enums.ResultCodeConstants;
import com.valmet.watermark.exception.WatermarkApplicationException;
import org.springframework.http.HttpStatus;

/**
 * WatermarkResponseUtil class for handling application-specific exceptions and
 * responses.
 * <p>
 * This class provides methods to throw exceptions based on specific result
 * codes and error scenarios encountered in the application. It is designed to
 * standardize error handling and response generation.
 *
 * @author BJIT
 * @version 1.0
 */
public class WatermarkResponseUtil {
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private WatermarkResponseUtil () {
    }

    /**
     * Throws a {@link WatermarkApplicationException} based on the provided result
     * code.
     * <p>
     * Maps the {@link ResultCodeConstants} to corresponding error codes and HTTP
     * statuses.
     *
     * @param authResultCode the result code that indicates the specific error
     *                       scenario
     * @return never returns a value, as an exception is always thrown
     * @throws WatermarkApplicationException with the appropriate error details
     *                                       based on the provided result code
     */
    public static WatermarkApplicationException throwApplicationException (ResultCodeConstants authResultCode) throws WatermarkApplicationException {
        switch (authResultCode) {
            case WRONG_CREDENTIALS:
                throw new WatermarkApplicationException (
                        authResultCode,
                        ErrorCode.WRONG_CREDENTIALS,
                        HttpStatus.BAD_REQUEST
                );
            case TOKEN_EXPIRED:
                throw new WatermarkApplicationException (
                        authResultCode,
                        ErrorCode.JWT_TOKEN_EXPIRED,
                        HttpStatus.BAD_REQUEST
                );
            case FILE_REQUIRED:
                throw new WatermarkApplicationException (
                        authResultCode,
                        ErrorCode.FILE_REQUIRED,
                        HttpStatus.BAD_REQUEST
                );
            case UNSUPPORTED_FILE_TYPE:
                throw new WatermarkApplicationException (
                        authResultCode,
                        ErrorCode.UNSUPPORTED_FILE_TYPE,
                        HttpStatus.BAD_REQUEST
                );
            case FILE_SIZE_LIMIT:
                throw new WatermarkApplicationException (
                        authResultCode,
                        ErrorCode.FILE_SIZE_LIMIT,
                        HttpStatus.BAD_REQUEST
                );
            case INVALID_PROPERTY_KEY:
                throw new WatermarkApplicationException (
                        authResultCode,
                        ErrorCode.INVALID_PROPERTY_KEY,
                        HttpStatus.BAD_REQUEST
                );
            default:
                // Default case for unhandled result codes
                throw WatermarkApplicationException.builder ().resultCode (ResultCodeConstants.INTERNAL_SERVER_ERROR)
                        .errorCode (ErrorCode.INTERNAL_SERVER_ERROR).build ();
        }
    }

}
