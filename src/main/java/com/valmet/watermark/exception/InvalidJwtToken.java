package com.valmet.watermark.exception;

import java.io.Serial;

/**
 * Custom exception class for handling invalid JWT token scenarios.
 * <p>
 * This exception is thrown when a JWT (JSON Web Token) is deemed invalid during
 * authentication or token validation processes. It extends
 * {@link RuntimeException}, providing a runtime exception that does not require
 * explicit handling in the method signature.
 * </p>
 *
 * @author BJIT
 * @version 1.0
 */
public class InvalidJwtToken extends RuntimeException {
    /**
     * Serial version UID for ensuring consistent serialization.
     */
    @Serial
    private static final long serialVersionUID = -294671188037098603L;

    /**
     * Constructs a new {@link InvalidJwtToken} exception with the specified detail
     * message.
     *
     * @param msg the detail message explaining the reason for the exception.
     */
    public InvalidJwtToken (String msg) {
        super (msg);
    }
}
