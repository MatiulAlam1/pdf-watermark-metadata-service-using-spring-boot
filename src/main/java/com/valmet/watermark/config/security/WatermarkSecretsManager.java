package com.valmet.watermark.config.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valmet.watermark.constants.Constants;
import com.valmet.watermark.dto.MetadataDto;
import com.valmet.watermark.dto.SecretDto;
import com.valmet.watermark.exception.InvalidJwtToken;
import com.valmet.watermark.service.VaultService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.vault.support.VaultResponse;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service class for managing watermark secrets.
 *
 * @author BJIT
 * @version 1.0
 */
@Slf4j
@Service
public class WatermarkSecretsManager {

    private final VaultService vaultService;
    private final ObjectMapper objectMapper;
    private final Map<String, String> rsaKeyCache = new ConcurrentHashMap<> ();

    /**
     * Constructor for WatermarkSecretsManager.
     *
     * @param vaultService the VaultService instance
     * @param objectMapper the ObjectMapper instance
     */
    public WatermarkSecretsManager (VaultService vaultService, ObjectMapper objectMapper) {
        this.vaultService = vaultService;
        this.objectMapper = objectMapper;
    }

    /**
     * Retrieves the SecretDto from the vault or cache.
     *
     * @return the SecretDto instance
     */
    public SecretDto getSecretDto () {
        log.info ("Attempting to retrieve SecretDto from vault.");
        SecretDto secretDto = vaultService.getRsaKeyDto ();
        if (secretDto != null && secretDto.getData () != null && secretDto.getMetadata () != null) {
            String privateKey = secretDto.getData ().get (Constants.PRIVATE_KEY);
            String publicKey = secretDto.getData ().get (Constants.PUBLIC_KEY);
            String version = String.valueOf (secretDto.getMetadata ().getVersion ());
            String userName = secretDto.getData ().get (Constants.WATERMARK_USERNAME);
            String password = secretDto.getData ().get (Constants.WATERMARK_PASSWORD);
            log.info ("rsaKeyDto:: version:{}", version);
            if (!privateKey.equals (rsaKeyCache.get (Constants.PRIVATE_KEY))) {
                rsaKeyCache.put (Constants.PRIVATE_KEY, privateKey);
            }
            if (!publicKey.equals (rsaKeyCache.get (Constants.PUBLIC_KEY))) {
                rsaKeyCache.put (Constants.PUBLIC_KEY, publicKey);
            }
            if (!version.equals (rsaKeyCache.get (Constants.VERSION))) {
                rsaKeyCache.put (Constants.VERSION, version);
            }
            if (!userName.equals (rsaKeyCache.get (Constants.WATERMARK_USERNAME))) {
                rsaKeyCache.put (Constants.WATERMARK_USERNAME, userName);
            }
            if (!password.equals (rsaKeyCache.get (Constants.WATERMARK_PASSWORD))) {
                rsaKeyCache.put (Constants.WATERMARK_PASSWORD, password);
            }

        } else {
            log.info ("SecretDto is null, building from cache.");
            Map<String, String> mapData = new HashMap<> ();
            mapData.put (Constants.PRIVATE_KEY, rsaKeyCache.getOrDefault (Constants.PRIVATE_KEY, ""));
            mapData.put (Constants.PUBLIC_KEY, rsaKeyCache.getOrDefault (Constants.PUBLIC_KEY, ""));
            mapData.put (Constants.WATERMARK_USERNAME, rsaKeyCache.getOrDefault (Constants.WATERMARK_USERNAME, ""));
            mapData.put (Constants.WATERMARK_PASSWORD, rsaKeyCache.getOrDefault (Constants.WATERMARK_PASSWORD, ""));
            secretDto = SecretDto.builder ()
                    .data (mapData)
                    .metadata (MetadataDto.builder ().version (Integer.parseInt (rsaKeyCache.getOrDefault (Constants.VERSION, "0"))).build ())
                    .build ();
        }
        return secretDto;
    }

    /**
     * Extracts the token from the HTTP request.
     *
     * @param request the HttpServletRequest instance
     * @return the extracted token or null if not found
     */
    public String extractTokenFromRequest (HttpServletRequest request) {
        log.info ("Extracting token from request.");
        String authorizationHeader = request.getHeader (Constants.AUTHENTICATION_HEADER_NAME);
        if (authorizationHeader == null) {
            log.warn ("Authorization header is null.");
            return null;
        }
        return authorizationHeader.startsWith ("Bearer ") ? authorizationHeader.substring (7) : null;
    }

    /**
     * Fetches the RSA public key for a specific version.
     *
     * @param version the version of the RSA key
     * @return the RSAPublicKey instance
     * @throws InvalidJwtToken if the public key cannot be fetched
     */
    public RSAPublicKey fetchPublicKey (int version) throws InvalidJwtToken {
        log.info ("Fetching public key for version: {}", version);
        try {
            String publicKey = getSpecificVersion (version);
            log.info ("Public key fetched for version {}: {}", version, publicKey != null ? "success" : "failure");
            return readPublicKey (publicKey);
        } catch (Exception e) {
            log.info ("Error fetching public key for version: {}", version + " " + e.getMessage ());
            throw new InvalidJwtToken ("Failed to fetch public key for version: " + version);
        }
    }

    /**
     * Retrieves the specific version of the RSA public key from the vault.
     *
     * @param version the version of the RSA key
     * @return the public key as a String
     */
    public String getSpecificVersion (int version) {
        log.info ("Retrieving specific version of RSA key: {}", version);
        VaultResponse vaultResponse = vaultService.getSpecificVersion (version);
        if (vaultResponse != null && vaultResponse.getData () != null) {
            Map<String, Object> secretData = (Map<String, Object>) vaultResponse.getData ().get ("data");
            String publicKey = (String) secretData.get (Constants.PUBLIC_KEY);
            if (publicKey != null && !publicKey.isEmpty ()) {
                log.info ("Public key found in vault for version {}:", version);
                return publicKey;
            }
        }
        log.warn ("No RSA public key found for version {}. Using public key of version {} from cache.", version, rsaKeyCache.getOrDefault (Constants.VERSION, "0"));
        return rsaKeyCache.getOrDefault (Constants.PUBLIC_KEY, "");
    }

    /**
     * Reads the RSA public key from a string.
     *
     * @param pubKey the public key as a String
     * @return the RSAPublicKey instance
     * @throws Exception if the public key cannot be read
     */
    public RSAPublicKey readPublicKey (String pubKey) throws Exception {
        log.info ("Reading public key.");
        try {
            byte[] keyBytes = java.util.Base64.getDecoder ().decode (pubKey.replaceAll ("\\s", ""));
            X509EncodedKeySpec spec = new X509EncodedKeySpec (keyBytes);
            KeyFactory kf = KeyFactory.getInstance ("RSA");
            PublicKey publicKey = kf.generatePublic (spec);
            log.info ("Successfully read public key.");
            return (RSAPublicKey) publicKey;
        } catch (Exception e) {
            log.error ("Error reading public key: {}", e.getMessage ());
            throw e;
        }
    }

    /**
     * Extracts the RSA key version from the JWT token.
     *
     * @param token the JWT token
     * @return the RSA key version as an Integer
     * @throws JsonProcessingException if the token cannot be processed
     */
    public Integer getVaultKeyVersionFromToken (String token) throws JsonProcessingException {
        log.info ("Extracting RSA key version from token.");
        String[] tokenParts = token.split ("\\.");
        if (tokenParts.length < 2) {
            log.error ("Invalid token format.");
            throw new InvalidJwtToken ("Invalid token format.");
        }
        String payloadJson = new String (Base64.getUrlDecoder ().decode (tokenParts[1]));
        Map<String, Object> payloadMap = objectMapper.readValue (payloadJson, Map.class);
        String rsaKeyVersion = payloadMap.getOrDefault (Constants.RSA_KEY_VERSION, "").toString ();
        log.info ("RSA key version extracted from token: {}", rsaKeyVersion);
        if (!rsaKeyVersion.isEmpty ()) {
            try {
                return Integer.parseInt (rsaKeyVersion);
            } catch (NumberFormatException e) {
                log.error ("Invalid RSA key version format: {}", rsaKeyVersion);
            }
        }
        return null;
    }
}