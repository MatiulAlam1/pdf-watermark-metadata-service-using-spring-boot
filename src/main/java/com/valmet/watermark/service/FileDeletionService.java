package com.valmet.watermark.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for scheduling and executing file deletions.
 */
@Slf4j
@Component
public class FileDeletionService {
    /**
     * The size of the thread pool for file deletion tasks.
     */
    @Value ("${file.deletion.thread.pool.size}")
    private static int threadPoolSize;
    /**
     * Executor service for scheduling file deletion tasks.
     */
    private static final ScheduledExecutorService fileDeletionExecutor = Executors.newScheduledThreadPool (threadPoolSize, new ThreadFactory () {
        private final ThreadFactory defaultFactory = Executors.defaultThreadFactory ();
        private final AtomicInteger threadNumber = new AtomicInteger (1); // Ensures thread-safe numbering

        @Override
        public Thread newThread (Runnable r) {
            Thread thread = defaultFactory.newThread (r);
            thread.setName ("FileDeletionThread-" + threadNumber.getAndIncrement ());
            return thread;
        }
    });
    @Value ("${watermark.file.delete.delay}")
    private int fileDeletionDelay;

    /**
     * Schedules the deletion of a file if it exists.
     *
     * @param filePath      the path of the file to delete
     * @param deleteMessage the message to log upon deletion
     */
    public void scheduleFileDeletionIfExists (String filePath, String deleteMessage) {
        File file = new File (filePath);
        log.info ("Scheduling deletion for file: {} ", file.getAbsolutePath ());
        if (file.exists ()) {
            scheduleFileDeletion (file, deleteMessage);
        } else {
            log.warn ("File does not exist: {}", file.getAbsolutePath ());
        }
    }

    /**
     * Schedules the deletion of a file.
     *
     * @param file          the file to delete
     * @param deleteMessage the message to log upon deletion
     */
    private void scheduleFileDeletion (File file, String deleteMessage) {
        fileDeletionExecutor.schedule (() -> {
            try {
                if (file.exists ()) {
                    if (file.delete ()) {
                        log.info ("{} File deleted successfully. File path: {}", deleteMessage, file.getAbsolutePath ());
                    } else {
                        log.error ("{} Failed to delete file. Retrying in 5 seconds. File path: {}", deleteMessage, file.getAbsolutePath ());
                        retryFileDeletion (file);
                    }
                } else {
                    log.warn ("File not found when attempting to delete: {}", file.getAbsolutePath ());
                }
            } catch (SecurityException e) {
                log.error ("Security exception while deleting file: {}. Retrying in 10 seconds.", file.getAbsolutePath (), e);
                retryFileDeletion (file);
            }
        }, fileDeletionDelay, TimeUnit.MILLISECONDS);
    }

    /**
     * Retries the deletion of a file after a specified delay.
     *
     * @param file the file to delete
     */
    private void retryFileDeletion (File file) {
        fileDeletionExecutor.schedule (() -> {
            if (file.exists () && file.delete ()) {
                log.info ("File deleted successfully after retry. File path: {}", file.getAbsolutePath ());
            } else {
                log.error ("Failed to delete file after retry. File path: {}", file.getAbsolutePath ());
            }
        }, 3, TimeUnit.SECONDS);
    }

    /**
     * Gracefully shuts down the file deletion executor when the application stops.
     */
    @PreDestroy
    public void shutdownExecutor () {
        log.info ("Shutting down file deletion executor...");
        fileDeletionExecutor.shutdown ();
        try {
            while (!fileDeletionExecutor.awaitTermination (5, TimeUnit.SECONDS)) {
                log.info ("Waiting for file deletion tasks to complete...");
            }
            log.info ("File deletion executor shut down successfully.");
        } catch (InterruptedException e) {
            log.error ("File deletion executor interrupted during shutdown.", e);
            fileDeletionExecutor.shutdownNow ();
        }
    }
}
