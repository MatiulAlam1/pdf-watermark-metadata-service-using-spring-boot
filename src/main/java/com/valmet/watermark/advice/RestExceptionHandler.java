package com.valmet.watermark.advice;

import com.valmet.watermark.constants.ErrorCode;
import com.valmet.watermark.constants.Messages;
import com.valmet.watermark.exception.InvalidJwtToken;
import com.valmet.watermark.exception.WatermarkApplicationException;
import com.valmet.watermark.logging.ErrorMessageInfo;
import com.valmet.watermark.logging.LogMessageConfig;
import com.valmet.watermark.response.BaseResponse;
import com.valmet.watermark.service.LdapService;
import jakarta.xml.bind.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import javax.naming.AuthenticationException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.valmet.watermark.enums.ResponseType.ERROR;

/**
 * Global exception handler for the application.
 * <p>
 * This class centralizes exception handling using {@link RestControllerAdvice},
 * allowing consistent and standardized error responses across all REST
 * controllers. It provides handlers for common exceptions, including custom
 * application exceptions and security-related exceptions.
 * </p>
 *
 * <p>
 * <b>Key Features:</b>
 * </p>
 * <ul>
 * <li>Handles validation, authentication, and JWT-related exceptions.</li>
 * <li>Logs detailed stack traces for debugging purposes.</li>
 * <li>Returns structured error responses using {@link BaseResponse}.</li>
 * </ul>
 *
 * <p>
 * The class is annotated with:
 * </p>
 * <ul>
 * <li>{@link RestControllerAdvice}: Enables global exception handling for REST
 * controllers.</li>
 * <li>{@link Order}: Specifies precedence in case multiple exception handlers
 * are defined.</li>
 * <li>{@link Slf4j}: Provides logging capabilities using SLF4J.</li>
 * </ul>
 *
 * @author BJIT
 * @version 1.0
 */
@RestControllerAdvice
@Order (Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RestExceptionHandler {
    private final LogMessageConfig logMessageConfig;

    /**
     * Constructor to inject dependencies.
     *
     * @param logMessageConfig configuration for logging error messages.
     */
    @Autowired
    public RestExceptionHandler (LogMessageConfig logMessageConfig) {
        this.logMessageConfig = logMessageConfig;
    }

    @ExceptionHandler ({ValidationException.class, RuntimeException.class, Exception.class})
    public ResponseEntity<?> handleCommonExceptions (Exception ex) {
        HttpStatus status = ex instanceof ValidationException ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
        String message = ex instanceof ValidationException ? ex.getMessage () : Messages.SERVER_ERROR;
        return buildResponseEntity (message, null, status, ex);
    }

    /**
     * Builds a structured error response with the given details.
     *
     * @param message   the error message.
     * @param errorCode the error code.
     * @param status    the HTTP status.
     * @param ex        the exception.
     * @return a {@link ResponseEntity} with the error details.
     */
    private ResponseEntity<Object> buildResponseEntity (String message, String errorCode, HttpStatus status,
                                                        Exception ex) {
        logger (message + " " + ex.getLocalizedMessage (), ex);
        return new ResponseEntity<> (BaseResponse.builder ().responseType (ERROR).message (Collections.singleton (message))
                .code (errorCode).build (), status);

    }

    /**
     * Logs detailed information about the exception, including stack trace.
     *
     * @param message the error message.
     * @param ex      the exception.
     */
    private void logger (String message, Exception ex) {
        StackTraceElement[] stackTrace = ex.getStackTrace ();
        StringBuilder str = new StringBuilder ();
        int i = 0;
        for (StackTraceElement stackTraceElement : stackTrace) {
            str.append (stackTraceElement.getFileName ()).append (", ").append (stackTraceElement.getLineNumber ())
                    .append (", ").append (stackTraceElement.getMethodName ()).append ("\n");
            if (++i == 5)
                break;
        }
        log.error ("{} {}", message, "This exception may occurred on \n" + str);
    }

    /**
     * Handles authentication-related exceptions.
     *
     * @param e the exception to handle.
     * @return a {@link BaseResponse} with the error details.
     */
    @ExceptionHandler ({AuthenticationException.class})
    public BaseResponse handle (Exception e) {
        logger (e.getLocalizedMessage (), e);
        return BaseResponse.builder ().responseType (ERROR).message (Collections.singleton (e.getMessage ()))
                .code (ErrorCode.WRONG_CREDENTIALS).build ();
    }

    /**
     * Handles {@link AccessDeniedException} and returns an UNAUTHORIZED response.
     *
     * @param e the exception to handle.
     * @return a {@link BaseResponse} with the error details.
     */
    @ExceptionHandler ({AccessDeniedException.class})
    @ResponseStatus (HttpStatus.UNAUTHORIZED)
    public BaseResponse handle (AccessDeniedException e) {
        return BaseResponse.builder ().responseType (ERROR).message (Collections.singleton (e.getMessage ()))
                .code (ErrorCode.ACCESS_DENIED).build ();
    }

    /**
     * Handles custom {@link InvalidJwtToken} exceptions.
     *
     * @param e the exception to handle.
     * @return a {@link BaseResponse} with the error details.
     */
    @ExceptionHandler ({InvalidJwtToken.class, JwtException.class})
    @ResponseStatus (HttpStatus.UNAUTHORIZED)
    public BaseResponse handle (InvalidJwtToken e) {
        logger (e.getLocalizedMessage (), e);
        return BaseResponse.builder ().responseType (ERROR).message (Collections.singleton (e.getMessage ()))
                .code (ErrorCode.INVALID_JWT_TOKEN).build ();
    }

    @ExceptionHandler ({LdapService.LdapException.class})
    @ResponseStatus (HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Object> handleAppError (LdapService.LdapException e) {
        logger (e.getLocalizedMessage (), e);
        return buildResponseEntity (e.getMessage (), null, HttpStatus.INTERNAL_SERVER_ERROR, e);
    }

    /**
     * Handles {@link WatermarkApplicationException} and builds a detailed error
     * response.
     *
     * @param ex the exception to handle.
     * @return a {@link ResponseEntity} with the error details.
     */
    @ExceptionHandler ({WatermarkApplicationException.class})
    public ResponseEntity<Object> handleAppError (WatermarkApplicationException ex) {
        ErrorMessageInfo errorMessageInfo = logMessageConfig.getErrorMessageInfo (ex.getErrorCode ());
        String message = errorMessageInfo.getMessageTemplate ();
        return buildResponseEntity (message, ex.getErrorCode (), ex.getStatus (), ex);
    }

    /**
     * Handles validation exceptions for method arguments.
     * <p>
     * This method captures {@link MethodArgumentNotValidException} thrown when a method argument
     * annotated with {@code @Valid} fails validation. It extracts the validation errors and returns
     * them in a structured format.
     * </p>
     *
     * @param ex the {@link MethodArgumentNotValidException} containing validation errors
     * @return a {@link ResponseEntity} containing a map of field names and their corresponding error messages,
     * with a {@link HttpStatus#BAD_REQUEST} status
     */
    @ExceptionHandler (MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions (MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<> ();
        ex.getBindingResult ().getFieldErrors ().forEach (error ->
                errors.put (error.getField (), error.getDefaultMessage ())
        );

        return new ResponseEntity<> (errors, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles exceptions when a required request part is missing.
     * <p>
     * This method captures {@link MissingServletRequestPartException} thrown when a required part of a multipart request is missing.
     * It logs the missing part name and returns a {@link ResponseEntity} with a {@link HttpStatus#BAD_REQUEST} status and an error message.
     * </p>
     *
     * @param ex the {@link MissingServletRequestPartException} containing details about the missing request part
     * @return a {@link ResponseEntity} with a {@link HttpStatus#BAD_REQUEST} status and an error message indicating the missing part
     */
    @ExceptionHandler (MissingServletRequestPartException.class)
    public ResponseEntity<?> handleMissingParams (MissingServletRequestPartException ex) {
        String name = ex.getRequestPartName ();
        log.error ("{} part is missing", name);
        return ResponseEntity.status (HttpStatus.BAD_REQUEST).body ("Request params '" + name + "' is missing");
    }
}
