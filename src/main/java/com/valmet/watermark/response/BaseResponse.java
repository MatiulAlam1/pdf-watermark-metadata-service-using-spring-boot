package com.valmet.watermark.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.valmet.watermark.enums.ResponseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;

/**
 * Data Transfer Object (DTO) for standardized API responses.
 * <p>
 * This class encapsulates the structure of a typical response returned by the
 * API. It includes fields for response type, messages, results, and status
 * codes.
 * </p>
 *
 * @author BJIT
 * @version 1.0
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class BaseResponse implements Serializable {
    /**
     * Serial version UID for ensuring consistent serialization.
     */
    @Serial
    private static final long serialVersionUID = 1546524853123L;
    /**
     * The type of the response.
     * <p>
     * Indicates the general nature of the response, such as {@code RESULT} or
     * {@code ERROR}.
     * </p>
     */
    @JsonProperty ("type")
    private ResponseType responseType;
    /**
     * The collection of messages associated with the response.
     * <p>
     * Typically used to provide additional information about the operation's
     * result. Example: {@code ["Operation successful", "Additional details..."]}
     * </p>
     */
    @JsonProperty ("message")
    private Collection<String> message;
    /**
     * The result or data payload of the response.
     * <p>
     * This field can contain any object representing the outcome of the operation.
     * Example: {@code { "id": 1, "name": "Sample Object" }}
     * </p>
     */
    @JsonProperty ("result")
    private Object result;
    /**
     * The status code of the response.
     * <p>
     * Indicates the outcome of the operation. For example: {@code "200"} for
     * success, {@code "400"} for bad requests, etc.
     * </p>
     */
    @JsonProperty ("code")
    private String code;
}
