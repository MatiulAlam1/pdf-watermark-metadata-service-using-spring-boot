package com.valmet.watermark.service;

import com.valmet.watermark.constants.Constants;
import com.valmet.watermark.dto.SecretDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;
import org.springframework.vault.support.VaultResponseSupport;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Service class for interacting with Vault to retrieve secrets.
 *
 * @author BJIT
 * @version 1.0
 */
@Service
@Slf4j
public class VaultService {
    private final VaultOperations vaultOperations;
    private final VaultTemplate vaultTemplate;
    @Value ("${spring.cloud.vault.kv.backend}")
    private String kvBackend;
    @Value ("${spring.cloud.vault.kv.application-name}")
    private String applicationName;

    /**
     * Constructor for VaultService.
     *
     * @param vaultOperations the VaultOperations instance for interacting with Spring Vault.
     * @param vaultTemplate   the VaultTemplate instance for interacting with Spring Vault.
     */
    public VaultService (VaultOperations vaultOperations, VaultTemplate vaultTemplate
    ) {
        this.vaultOperations = vaultOperations;
        this.vaultTemplate = vaultTemplate;
    }

    /**
     * Retrieves the RSA key from Vault as a SecretDto.
     *
     * @return the SecretDto containing the RSA key, or null if an error occurs.
     */
    public SecretDto getRsaKeyDto () {
        try {
            String secretPath = kvBackend + "/" + Constants.DATA + "/" + applicationName;
            log.info ("Attempting to retrieve RSA key from Vault at path: {}", secretPath);
            if (vaultOperations != null) {
                VaultResponseSupport<SecretDto> response = vaultOperations.read (secretPath, SecretDto.class);
                log.info ("Successfully retrieved RSA key from Vault.");
                return response.getData ();
            }
            log.info ("VaultOperations is null");
            return null;
        } catch (Exception e) {
            log.error ("Error occurred while reading RSA key from Vault: {}", e.getMessage (), e);
            return null;
        }

    }

    /**
     * Retrieves a specific version of a secret from Vault.
     *
     * @param version the version number of the secret to retrieve.
     * @return the LogicalResponse containing the secret data, or null if an error occurs.
     */
    public VaultResponse getSpecificVersion (int version) {
        try {
            String encodedKvBackend = java.net.URLEncoder.encode (kvBackend, StandardCharsets.UTF_8);
            String encodedApplicationName = java.net.URLEncoder.encode (applicationName, StandardCharsets.UTF_8);
            log.info ("Encoded KV Backend: {}", encodedKvBackend);
            log.info ("Encoded Application Name: {}", encodedApplicationName);
            return getSpecificVersion (encodedKvBackend, encodedApplicationName, version);
        } catch (Exception e) {
            log.error ("Error occurred while reading secret from Vault: {}", e.getMessage ());
            return null;
        }
    }

    /**
     * Retrieves a specific version of a secret from Vault.
     *
     * @param kvBackend       the encoded KV backend path.
     * @param applicationName the encoded application name.
     * @param version         the version number of the secret to retrieve.
     * @return the VaultResponse containing the secret data, or null if an error occurs.
     */
    private VaultResponse getSpecificVersion (String kvBackend, String applicationName, int version) {
        try {
            String encodedKvBackend = URLEncoder.encode (kvBackend, StandardCharsets.UTF_8) + "/data";
            String encodedApplicationName = URLEncoder.encode (applicationName, StandardCharsets.UTF_8);
            String path = String.format ("%s/%s?version=%s", encodedKvBackend, encodedApplicationName, version);
            log.info ("Retrieving secret from Vault at path: {}", path);
            VaultResponse response = vaultTemplate.read (path);
            log.info ("Successfully retrieved secret from Vault.");
            return response;
        } catch (Exception e) {
            log.error ("Error reading specific version {} of secret from Vault: {}", version, e.getMessage (), e);
            return null;
        }
    }
}