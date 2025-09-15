package com.valmet.watermark.exception;

import com.valmet.watermark.enums.ResultCodeConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Custom exception class for handling application-specific errors in the
 * watermark service.
 * <p>
 * This exception extends {@link RuntimeException} and is designed to
 * encapsulate detailed error information, including result codes, error codes,
 * HTTP status, and optional arguments for error context.
 * </p>
 *
 * @author BJIT
 * @version 1.0
 */
@Getter
@Setter
@AllArgsConstructor
@Builder
public class WatermarkApplicationException extends RuntimeException {

    /**
     * Serial version UID for ensuring consistent serialization.
     */
    @Serial
    private static final long serialVersionUID = 1436995162658277359L;
    /**
     * The result code representing the specific error scenario.
     */
    private ResultCodeConstants resultCode;
    /**
     * The error code associated with the exception.
     */
    private String errorCode;
    /**
     * Additional arguments providing context or details about the error.
     * <p>
     * This field is marked as {@code transient} to exclude it from serialization.
     * </p>
     */
    private transient Object[] args;

    @Builder.Default
    private HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

    /**
     * Constructs a new {@link WatermarkApplicationException} with the specified
     * details.
     *
     * @param resultCode the result code representing the error scenario.
     * @param errorCode  the specific error code for this exception.
     * @param status     the HTTP status associated with the exception.
     * @param args       additional arguments providing context for the error.
     */
    public WatermarkApplicationException (ResultCodeConstants resultCode, String errorCode, HttpStatus status,
                                          Object... args) {
        super ("Result Code=" + resultCode + ", Error Code=" + errorCode);
        this.resultCode = resultCode;
        this.errorCode = errorCode;
        this.status = status;
        this.args = args;
    }
}
