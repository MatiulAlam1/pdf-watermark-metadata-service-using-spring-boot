package com.valmet.watermark.controller;

import com.valmet.watermark.exception.WatermarkApplicationException;
import com.valmet.watermark.response.BaseResponse;
import com.valmet.watermark.service.AddWaterMarkToPdfService;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.valmet.watermark.enums.ResponseType.ERROR;

/**
 * AddWaterMarkToPdfController class for handling requests to add watermarks to
 * PDF files.
 * <p>
 * This controller provides an API endpoint to upload PDF files and apply
 * watermarks based on optional metadata such as person ID and system.
 * </p>
 *
 * @author BJIT
 * @version 1.0
 */
@RestController
@RequestMapping ("/api")
@Tag (name = "PDF Watermark API")
@Slf4j
@SecurityRequirement (name = "Bearer Authentication")
public class AddWaterMarkToPdfController {
    private static final String CIRCUIT_BREAKER_NAME = "watermark-api";
    private final AddWaterMarkToPdfService addWaterMarkToPdfService;
    private final HttpServletRequest request;

    /**
     * Constructor to initialize the watermark service.
     *
     * @param addWaterMarkToPdfService the service responsible for processing and adding
     *                                 watermarks to PDF files
     */
    public AddWaterMarkToPdfController (AddWaterMarkToPdfService addWaterMarkToPdfService, HttpServletRequest request) {
        this.addWaterMarkToPdfService = addWaterMarkToPdfService;
        this.request = request;
    }

    /**
     * Endpoint to upload PDF files and add watermarks.
     * <p>
     * This method accepts multiple PDF files and optional metadata (person ID and
     * system) to apply watermarks to the files.
     * </p>
     *
     * @param files    a list of {@link MultipartFile} objects representing the PDF
     *                 files to be watermarked
     * @param personID an optional string representing the person ID for watermark
     *                 metadata
     * @param system   an optional string representing the system for watermark
     *                 metadata
     * @return a {@link ResponseEntity} containing the result of the watermarking
     * operation
     * @throws IOException if an error occurs during file processing
     */
    @PostMapping (value = "/watermark", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CircuitBreaker (name = CIRCUIT_BREAKER_NAME, fallbackMethod = "uploadFilesFallback")
    @Bulkhead (name = CIRCUIT_BREAKER_NAME, type = Bulkhead.Type.SEMAPHORE)
    @Operation (summary = "Add watermark to PDF files.", description = "This endpoint allows users to upload PDF files and apply watermarks based on input parameters such as Person ID and System.")
    public CompletableFuture<ResponseEntity<?>> uploadFilesAndAddWatermark(
	    @RequestParam("file") List<MultipartFile> files,
	    @RequestParam(value = "personID", required = false) String personID,
	    @RequestParam(value = "system", required = false) String system,
	    @RequestParam(value = "email", required = false) String email) throws IOException {

        String ipAddress = request.getHeader ("X-Forwarded-For");
        log.info ("User IP Address X-Forwarded-For: {}", ipAddress);
        if (ipAddress == null || ipAddress.isEmpty () || "unknown".equalsIgnoreCase (ipAddress)) {
            ipAddress = request.getRemoteAddr ();
        }

        String clientAppName = request.getHeader ("X-Client-Application-Name");
        log.info ("Request sender: {}", clientAppName);
        if (system == null || system.isEmpty ()) {
            system = clientAppName;
        }
        log.info ("User IP Address: {}, Client System: {}", ipAddress, system);
        return addWaterMarkToPdfService.getWatermarkedPdfAsync (files, personID, system, email);
    }

    public CompletableFuture<ResponseEntity<?>> uploadFilesFallback (List<MultipartFile> files, String personID, String system, String email, Throwable throwable) {
        log.error ("Fallback method triggered: {}", throwable.getMessage ());
        if (throwable instanceof WatermarkApplicationException) {
            throw (WatermarkApplicationException) throwable;
        }
        try {
            return addWaterMarkToPdfService.getInputPDFAsync (files);
        } catch (Exception e) {
            log.error ("Error occurred while processing the input PDF files: {}", e.getMessage ());
            return CompletableFuture.completedFuture (new ResponseEntity<> (BaseResponse.builder ().responseType (ERROR).message (Collections.singletonList ("Error occurred while processing the input PDF files.")).code (HttpStatus.INTERNAL_SERVER_ERROR.toString ()).build (), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }
}