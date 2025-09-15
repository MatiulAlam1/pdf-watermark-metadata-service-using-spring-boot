package com.valmet.watermark.service;

import com.valmet.watermark.constants.Constants;
import com.valmet.watermark.enums.ResultCodeConstants;
import com.valmet.watermark.exception.WatermarkApplicationException;
import com.valmet.watermark.service.impl.AddWaterMarkToPdfServiceImpl;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static com.valmet.watermark.response.WatermarkResponseUtil.throwApplicationException;

/**
 * AddWaterMarkToPdfService for handling PDF watermarking operations.
 * <p>
 * This service provides functionalities to upload, process, and add watermark
 * as a image to PDF files, including the creation of ZIP archives for multiple
 * files.
 * </p>
 *
 * @author BJIT
 * @version 1.0
 */

@Service
@Slf4j
public class AddWaterMarkToPdfService {
    private static final Logger requestResponseLogger = LoggerFactory.getLogger ("REQUEST_RESPONSE_LOGGER");
    private static final String WATERMARK_SEPARATOR = "_watermark_";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat ("dd-MM-yyyy");
    private static final ScheduledExecutorService fileDeletionExecutor = Executors.newScheduledThreadPool (10);
    private final AddWaterMarkToPdfServiceImpl addWaterMarkToPdfServiceImpl;
    private final String RETRY_NAME = "watermark-api";
    private final FileDeletionService fileDeletionService;
    private final LdapService ldapService;
    @Value ("${watermark.file.upload.directory}")
    private String uploadDir;
    @Value ("${watermark.file.name}")
    private String fileName;
    @Value ("${watermark.downloader.message}")
    private String strDownloadMessage;
    @Value ("${watermark.downloader.id}")
    private String strDownloaderIdLabel;
    @Value ("${watermark.download.date}")
    private String strDownloadDateLabel;
    @Value ("${watermark.system}")
    private String strSystemLabel;

    /**
     * Constructor to initialize the watermarking implementation service.
     *
     * @param addWaterMarkToPdfServiceImpl the service implementation for adding
     *                                     watermarks to PDF files
     */
    public AddWaterMarkToPdfService (AddWaterMarkToPdfServiceImpl addWaterMarkToPdfServiceImpl, FileDeletionService fileDeletionService, LdapService ldapService) {
        this.addWaterMarkToPdfServiceImpl = addWaterMarkToPdfServiceImpl;
        this.fileDeletionService = fileDeletionService;
        this.ldapService = ldapService;
    }

    /**
     * Asynchronously processes uploaded PDF files, applies watermarks, and returns a response.
     *
     * @param file        list of uploaded PDF files as {@link MultipartFile}
     * @param strPersonID optional person ID for watermark metadata
     * @param strSystem   optional system metadata for the watermark
     * @return a {@link CompletableFuture} containing the {@link ResponseEntity} with the watermarked file or a ZIP archive of files
     * @throws IOException if an error occurs during file handling
     */
    @Async ("taskExecutor")
    public CompletableFuture<ResponseEntity<?>> getWatermarkedPdfAsync (List<MultipartFile> file, String strPersonID, String strSystem, String strEmail) throws IOException {
        return CompletableFuture.completedFuture (getWatermarkedPdf (file, strPersonID, strSystem, strEmail));
    }

    /**
     * Processes uploaded PDF files, applies watermarks, and returns a response.
     * <p>
     * If only one file is uploaded, a watermarked file is returned as PDF. For
     * multiple files, a ZIP archive of watermarked files is generated and returned
     * a ZIP.
     * </p>
     *
     * @param file        list of uploaded PDF files as {@link MultipartFile}
     * @param strPersonID optional person ID for watermark metadata
     * @param strSystem   optional system metadata for the watermark
     * @return a {@link ResponseEntity} containing the watermarked file or a ZIP
     * archive of files
     * @throws IOException if an error occurs during file handling
     */
    @Retry (name = RETRY_NAME)
    public ResponseEntity<?> getWatermarkedPdf (List<MultipartFile> file, String strPersonID, String strSystem, String strEmail) throws IOException {
        log.info ("Inside getWatermarkedPdf method");
        File watermarkedFile = null;
        if (file == null || file.isEmpty () || file.get (0).isEmpty ()) {
            throwApplicationException (ResultCodeConstants.FILE_REQUIRED);
        }
        File uploadDirFile = new File (uploadDir);
        if (!uploadDirFile.exists () && !uploadDirFile.mkdirs ()) {
            log.error ("Failed to create upload directory in getWatermarkedPdf: {}", uploadDir);
            return ResponseEntity.status (HttpStatus.INTERNAL_SERVER_ERROR).body ("File directory not found");
        }
        log.info ("System defined upload path: {}, created uploaded file path: {}", uploadDir, uploadDirFile.getPath ());

        // File validation
        validateFiles (file);

        //Prepare custom metadata for watermark pdf file
        Map<String, String> mapPdfCustomProperties = new HashMap<> ();
        String strKeyWords;
        String strWaterMark = "";
        Date date = new Date ();
        String strCurrentDate = Constants.dateFormat.format (date);
        mapPdfCustomProperties.put (strDownloadDateLabel, strCurrentDate);
        strKeyWords = strDownloadDateLabel + ": " + strCurrentDate;
        if (isEmpty(strPersonID) && isNotEmpty(strEmail)) {
            long startTime = System.currentTimeMillis();
            log.info("Deriving person ID from email: {}", strEmail);
            strPersonID = ldapService.findUserPrincipalNameByEmail(strEmail, Constants.PERSON_ID);
            log.info("Time taken to derive person ID from email: {} ms", System.currentTimeMillis() - startTime);
            log.info("Person ID derived from email: {}", strPersonID);
        }
        if (isNotEmpty(strPersonID)) {
            strWaterMark = strDownloadMessage + " " + strPersonID;
            mapPdfCustomProperties.put(strDownloaderIdLabel, strPersonID);
            strKeyWords = strKeyWords + ", " + strDownloaderIdLabel + ": " + strPersonID;
        }
        if (isNotEmpty (strSystem)) {
            strWaterMark = !strWaterMark.isEmpty () ? strWaterMark + "/" + strSystem : strDownloadMessage + " " + strSystem;
            mapPdfCustomProperties.put (strSystemLabel, strSystem);
            strKeyWords = strKeyWords + ", " + strSystemLabel + ": " + strSystem;
        }
        log.info ("Keyword info: {}", strKeyWords);

        String dateTime = dateFormat.format (date) + Constants.UNDER_SCORE + date.getTime ();
        String randomString = "";
        // Save the original file
        if (file.size () == 1) {
            MultipartFile multipartFile = file.get (0);
            String originalFilename = multipartFile.getOriginalFilename ();
            log.info ("Original file name:{}", originalFilename);
            requestResponseLogger.info ("Original file name:{}", originalFilename);
            // File transfer to system provided directory
            File originalFile = new File (uploadDir + dateTime + WATERMARK_SEPARATOR + originalFilename);
            try {
                multipartFile.transferTo (originalFile);
            } catch (IOException e) {
                log.error ("Failed to transfer file: {}", originalFilename, e);
                fileDeletionService.scheduleFileDeletionIfExists (originalFile.getAbsolutePath (), "Input");
            }
            // Add watermarked to PDF
            randomString = RandomStringUtils.randomAlphanumeric (16);
            addWaterMarkToPdfServiceImpl.addWatermarkToExistingPdf (originalFile.getPath (), uploadDir + dateTime + Constants.UNDER_SCORE + originalFilename, strKeyWords, mapPdfCustomProperties, strWaterMark, uploadDir + randomString + ".png");

            try {
                watermarkedFile = new File (uploadDir + dateTime + Constants.UNDER_SCORE + originalFilename);
                log.info ("Watermarked output file :{}", watermarkedFile.getName ());
                requestResponseLogger.info ("Watermarked output file :{}", watermarkedFile.getName ());
                FileSystemResource fileResource = new FileSystemResource (watermarkedFile);
                HttpHeaders headers = new HttpHeaders ();
                headers.add (HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + originalFilename);
                headers.setContentType (MediaType.APPLICATION_PDF);
                return ResponseEntity.ok ().headers (headers).contentLength (watermarkedFile.length ()).contentType (MediaType.APPLICATION_PDF).body (fileResource);
            } catch (Exception e) {
                log.error ("Failed to create watermarked file: {}", originalFilename, e);
                return ResponseEntity.status (HttpStatus.INTERNAL_SERVER_ERROR).body ("Failed to create watermarked file");
            } finally {
                log.info ("Finally block executed in getWatermarkedPdf method");
                fileDeletionService.scheduleFileDeletionIfExists (watermarkedFile.getAbsolutePath (), "Output");
            }

        } else {
            List<String> fileNames = file.stream ().map (multipartFile -> transferFileToServerPath (multipartFile, uploadDir, dateTime)).filter (fileName -> fileName != null && !fileName.isEmpty ()).toList ();

            for (String fileName : fileNames) {
                randomString = RandomStringUtils.randomAlphanumeric (16);
                addWaterMarkToPdfServiceImpl.addWatermarkToExistingPdf (uploadDir + dateTime + Constants.UNDER_SCORE + fileName, uploadDir + dateTime + WATERMARK_SEPARATOR + fileName, strKeyWords, mapPdfCustomProperties, strWaterMark, uploadDir + randomString + ".png");
            }

            ByteArrayOutputStream zipOutput = new ByteArrayOutputStream ();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream (zipOutput)) {
                for (String filename : fileNames) {
                    File convertedFile = new File (uploadDir + dateTime + WATERMARK_SEPARATOR + filename);
                    zipOutputStream.putNextEntry (new ZipEntry (filename));
                    FileInputStream fileInputStream = new FileInputStream (convertedFile);
                    IOUtils.copy (fileInputStream, zipOutputStream);
                    fileInputStream.close ();
                    zipOutputStream.closeEntry ();
                    fileDeletionService.scheduleFileDeletionIfExists (convertedFile.getAbsolutePath (), "Output");
                }
                zipOutputStream.finish ();
            }
            InputStreamResource resource = new InputStreamResource (new ByteArrayInputStream (zipOutput.toByteArray ()));
            String zipFile = fileName + dateTime + ".zip";
            log.info ("Watermarked output zip file :{}", zipFile);
            requestResponseLogger.info ("Watermarked output zip file :{}", zipFile);
            return ResponseEntity.ok ().header (HttpHeaders.CONTENT_DISPOSITION, "attachment; " + "filename=" + zipFile).contentType (MediaType.parseMediaType ("application/zip")).body (resource);
        }
    }

    /**
     * Validates uploaded files for size and content type.
     *
     * @param files list of uploaded files as {@link MultipartFile}
     * @throws WatermarkApplicationException if any file is invalid or exceeds the
     *                                       size limit
     */
    public void validateFiles (List<MultipartFile> files) {
        for (MultipartFile multipartFile : files) {
            if (!isPDFContentType (multipartFile)) {
                log.error ("Unsupported file type: {}", multipartFile.getContentType ());
                throwApplicationException (ResultCodeConstants.UNSUPPORTED_FILE_TYPE);
            }
        }
    }

    /**
     * Transfers an uploaded file to the server's specified directory.
     *
     * @param file      the uploaded file as {@link MultipartFile}
     * @param uploadDir the directory where the file should be stored
     * @return the name of the stored file
     * @throws RuntimeException if the file cannot be stored
     */
    public String transferFileToServerPath (MultipartFile file, String uploadDir, String dateTime) {
        Path uploadPath = null;
        try {
            String originalFileName = StringUtils.cleanPath (Objects.requireNonNull (file.getOriginalFilename ()));
            // log.info ("Uploaded original file name:{}", originalFileName);
            log.info ("Uploaded original file name: {}", originalFileName);
            requestResponseLogger.info ("Uploaded original file name: {}", originalFileName);
            uploadPath = Paths.get (uploadDir + dateTime + Constants.UNDER_SCORE + originalFileName);
            log.info ("Upload path: {}", uploadPath);
            if (!Files.exists (uploadPath)) {
                Files.copy (file.getInputStream (), uploadPath, StandardCopyOption.REPLACE_EXISTING);
                return originalFileName;
            }
            return "";
        } catch (IOException e) {
            log.error ("Could not store file {}. Please try again!", file.getOriginalFilename (), e);
            if (uploadPath != null && Files.exists (uploadPath)) {
                try {
                    Files.delete (uploadPath);
                    log.info ("Deleted file due to upload error: {}", uploadPath);
                } catch (IOException deleteException) {
                    log.error ("Failed to delete file after upload error: {}", uploadPath, deleteException);
                }
            }
            throw new RuntimeException ("Could not store file " + file.getOriginalFilename () + ". Please try again!", e);
        }
    }

    /**
     * Checks if the uploaded file is a valid PDF.
     *
     * @param file the uploaded file as {@link MultipartFile}
     * @return true if the file is a PDF; false otherwise
     */
    public boolean isPDFContentType (MultipartFile file) {
        String cleanedFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        boolean isPDF = "application/pdf".equalsIgnoreCase (file.getContentType ());
        log.info ("File {} is PDF: {}", cleanedFileName, isPDF);
        return isPDF;
    }

    /**
     * Asynchronously processes uploaded PDF files and returns a response.
     *
     * @param file list of uploaded PDF files as {@link MultipartFile}
     * @return a {@link CompletableFuture} containing the {@link ResponseEntity} with the uploaded file or a ZIP archive of files
     * @throws IOException if an error occurs during file handling
     */
    public CompletableFuture<ResponseEntity<?>> getInputPDFAsync (List<MultipartFile> file) throws IOException {
        return CompletableFuture.completedFuture (getInputPDF (file));
    }

    /**
     * This method is called when API is failed to generate watermark and throw exception.
     * Processes uploaded PDF files, and returns a response.
     * <p>
     * If only one file is uploaded, a file is returned as PDF. For
     * multiple files, a ZIP archive of files is generated and returned
     * a ZIP.
     * </p>
     *
     * @param file list of uploaded PDF files as {@link MultipartFile}
     * @return a {@link ResponseEntity} containing the watermarked file or a ZIP
     * archive of files
     * @throws IOException if an error occurs during file handling
     */
    public ResponseEntity<?> getInputPDF (List<MultipartFile> file) throws IOException {
        log.info ("Inside getInputPDF method");
        File uploadDirFile = new File (uploadDir);
        if (!uploadDirFile.exists () && !uploadDirFile.mkdirs ()) {
            log.error ("Failed to create upload directory: {}", uploadDir);
            return ResponseEntity.status (HttpStatus.INTERNAL_SERVER_ERROR).body ("File directory not found");
        }
        Date date = new Date ();
        String dateTime = dateFormat.format (date) + Constants.UNDER_SCORE + date.getTime ();

        // Save the original file
        if (file.size () == 1) {
            MultipartFile multipartFile = file.get (0);
            String originalFilename = multipartFile.getOriginalFilename ();
            log.info ("Original file name in GetInputPDF: {}", originalFilename);
            // File transfer to system provided directory
            File originalFile = new File (uploadDir + dateTime + Constants.UNDER_SCORE + originalFilename);
            try {
                multipartFile.transferTo (originalFile);
            } catch (IOException e) {
                log.error ("Failed to transfer file: {}", originalFilename, e);
                fileDeletionService.scheduleFileDeletionIfExists (originalFile.getAbsolutePath (), "GetInputPDF method Input");
            }

            try {
                FileSystemResource fileResource = new FileSystemResource (originalFile);
                HttpHeaders headers = new HttpHeaders ();
                headers.add (HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + originalFile.getName ());
                headers.setContentType (MediaType.APPLICATION_PDF);
                return ResponseEntity.ok ().headers (headers).contentLength (originalFile.length ()).contentType (MediaType.APPLICATION_PDF).body (fileResource);
            } catch (Exception e) {
                log.error ("Failed to create watermarked file: {}", originalFilename, e);
                return ResponseEntity.status (HttpStatus.INTERNAL_SERVER_ERROR).body ("Failed to create watermarked file");
            } finally {
                log.info ("Finally block executed in GetInputPDF method");
                fileDeletionService.scheduleFileDeletionIfExists (originalFile.getAbsolutePath (), "GetInputPDF method Input");
            }

        } else {
            List<String> fileNames = file.stream ().map (multipartFile -> transferFileToServerPath (multipartFile, uploadDir, dateTime)).filter (fileName -> fileName != null && !fileName.isEmpty ()).toList ();
            ByteArrayOutputStream zipOutput = new ByteArrayOutputStream ();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream (zipOutput)) {
                for (String filename : fileNames) {
                    File convertedFile = new File (uploadDir + dateTime + Constants.UNDER_SCORE + filename);
                    zipOutputStream.putNextEntry (new ZipEntry (filename));
                    FileInputStream fileInputStream = new FileInputStream (convertedFile);
                    IOUtils.copy (fileInputStream, zipOutputStream);
                    fileInputStream.close ();
                    zipOutputStream.closeEntry ();
                    fileDeletionService.scheduleFileDeletionIfExists (convertedFile.getAbsolutePath (), "GetInputPDF method Input");
                }
                zipOutputStream.finish ();
            }
            InputStreamResource resource = new InputStreamResource (new ByteArrayInputStream (zipOutput.toByteArray ()));
            return ResponseEntity.ok ().header (HttpHeaders.CONTENT_DISPOSITION, "attachment; " + "filename=" + fileName + dateTime + ".zip").contentType (MediaType.parseMediaType ("application/zip")).body (resource);
        }
    }
}
