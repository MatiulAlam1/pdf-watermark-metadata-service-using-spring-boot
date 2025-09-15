package com.valmet.watermark.service;

import com.valmet.watermark.dto.MicroSoftADUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.ldap.CommunicationException;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

@Slf4j
@Service
public class LdapService {

    // AD-specific schema attributes
    private static final String SAM_ACCOUNT_NAME_ATTR = "sAMAccountName";
    private static final String USER_PRINCIPAL_NAME_ATTR = "userPrincipalName";
    private static final String MEMBER_OF_ATTR = "memberOf";
    private static final String DISPLAY_NAME_ATTR = "displayName";
    private static final String MAIL_ATTR = "mail";
    private final LdapTemplate ldapTemplate;

    /**
     * Constructor for LdapService.
     *
     * @param ldapTemplate the LdapTemplate to use for LDAP operations
     */
    @Autowired
    public LdapService (LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }


    /**
     * Find a user by sAMAccountName (username) and return enriched details.
     *
     * @param username the sAMAccountName of the user to find
     * @return an Optional containing the found MicroSoftADUser, or empty if not found
     */
    public Optional<MicroSoftADUser> findUserByUsername (String username) {
        try {

            LdapQuery query = query ().filter ("(&(objectClass=user)(sAMAccountName={0}))", username);
            MicroSoftADUser user = ldapTemplate.searchForObject (query, new ContextMapper<MicroSoftADUser> () {
                @Override
                public MicroSoftADUser mapFromContext (Object ctx) {
                    return mapToAdUser ((DirContextOperations) ctx);
                }
            });
            return Optional.ofNullable (user);
        } catch (CommunicationException e) {
            log.error ("Failed to communicate with LDAP server", e);
            throw new LdapException ("Failed to communicate with LDAP server", e);
        } catch (Exception e) {
            log.error ("An error occurred while searching for user", e);
            return Optional.empty ();
        }
    }

    /**
     * Extract group CNs from memberOf attribute.
     *
     * @param ctx the DirContextOperations containing the LDAP entry
     * @return a list of group CNs
     */
    private MicroSoftADUser mapToAdUser (DirContextOperations ctx) {
        return MicroSoftADUser.builder ()
                .username (ctx.getStringAttribute (SAM_ACCOUNT_NAME_ATTR))
                .upn (ctx.getStringAttribute (USER_PRINCIPAL_NAME_ATTR))
                .displayName (ctx.getStringAttribute (DISPLAY_NAME_ATTR))
                .email (ctx.getStringAttribute (MAIL_ATTR))
                .groups (extractGroups (ctx)).dn (ctx.getDn ().toString ())
                .build ();
    }

    /**
     * Extract group CNs from memberOf attribute.
     */
    private List<String> extractGroups (DirContextOperations ctx) {
        String[] memberOf = ctx.getStringAttributes (MEMBER_OF_ATTR);
        if (memberOf == null) return Collections.emptyList ();

        return Arrays.stream (memberOf).map (this::parseGroupCn).collect (Collectors.toList ());
    }

    /**
     * Parse CN from group DN (e.g., "CN=Admins,OU=Groups,DC=example,DC=com" → "Admins").
     */
    private String parseGroupCn (String groupDn) {
        return groupDn.split (",")[0].split ("=")[1];
    }

    /**
     * Authenticate a user against the LDAP server.
     *
     * @param username the username to authenticate
     * @param password the password to authenticate
     * @return true if authentication succeeded, false otherwise
     */
    public boolean authenticate (String username, String password) {
        try {
            // Construct DN for the user
            String userDn = "cn=" + username + ",OU=JAR,OU=Finland,OU=External Users,OU=Root2,DC=vstage,DC=co";

            // Authenticate
            DirContext context = ldapTemplate.getContextSource ().getContext (userDn, password);
            context.close (); // Close the context after use
            return true; // Authentication succeeded
        } catch (Exception e) {
            log.error ("Authentication failed", e);
            // Authentication failed
            return false;
        }
    }

    /**
     * Authenticate a user against Microsoft Active Directory.
     *
     * @param username the username to authenticate
     * @param password the password to authenticate
     * @return true if authentication succeeded, false otherwise
     */
    public boolean authenticateToMicrosoftAD (String username, String password) {
        try {
            String downLevelLogon = "VSTAGE\\" + username;
            DirContext context = ldapTemplate.getContextSource ().getContext (downLevelLogon, password);
            context.close ();
            return true;
        } catch (Exception e) {
            log.warn("Authentication failed for user: {} - {}", username, e.getMessage());
            return false;
        }
    }

    /**
     * Authenticate a user using UPN (User Principal Name).
     *
     * @param userPrincipalName the UPN to authenticate
     * @param password          the password to authenticate
     * @return true if authentication succeeded, false otherwise
     */
    public boolean authenticateWithUPN (String userPrincipalName, String password) {
        try {
            // Authenticate using UPN (email-like format)
            DirContext context = ldapTemplate.getContextSource ().getContext (userPrincipalName, password);
            context.close ();
            return true; // Success
        } catch (Exception e) {
            log.warn("Authentication failed for user: {} - {}", userPrincipalName, e.getMessage());
            return false; // Failure
        }
    }

    /**
     * Authenticate a user using email.
     *
     * @param email    the email to authenticate
     * @param password the password to authenticate
     * @return true if authentication succeeded, false otherwise
     */
    public boolean authenticateWithEmail(String email, String password) {
        try {
            String upn = findUserPrincipalNameByEmail(email, USER_PRINCIPAL_NAME_ATTR); // Find UPN from email
            if (upn == null) {
                log.warn("User with email {} not found in LDAP", email);
                return false;
            }

            // Authenticate with UPN
            DirContext context = ldapTemplate.getContextSource().getContext(upn, password);
            context.close();
            return true;
        } catch (Exception e) {
            log.error("Authentication failed for email: {} - {}", email, e.getMessage());
            return false;
        }
    }

    /**
     * Finds a user's attribute (such as userPrincipalName) in LDAP by their email address.
     * <p>
     * The result is cached using the email and attribute as the cache key.
     *
     * @param email     the email address to search for
     * @param attribute the LDAP attribute to retrieve (e.g., "userPrincipalName")
     * @return the value of the specified attribute if found, or {@code null} if not found or on error
     */
    @Cacheable(value = "emailToPersonId", key = "#email + '_' + #attribute")
    public String findUserPrincipalNameByEmail (String email, String attribute) {
        try {
            log.info("Finding userPrincipalName for email: {}", email+", attribute"+attribute);
            LdapQuery query = query ().filter ("(&(objectClass=user)(mail={0}))", email);
            return ldapTemplate.searchForObject (query, new ContextMapper<String> () {
                @Override
                public String mapFromContext (Object ctx) throws NamingException {
                    return ((DirContextOperations) ctx).getStringAttribute (attribute);
                }
            });
        } catch (Exception e) {
            log.error ("Failed to find userPrincipalName for email: {} - {}", email, e.getMessage());
            return null;
        }
    }


    /**
     * Custom exception for LDAP errors.
     */
    public static class LdapException extends RuntimeException {
        /**
         * Constructor for LdapException.
         *
         * @param message the error message
         * @param cause   the cause of the error
         */
        public LdapException (String message, Throwable cause) {
            super (message, cause);
        }
    }
}