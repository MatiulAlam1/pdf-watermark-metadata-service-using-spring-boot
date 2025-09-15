package com.valmet.watermark.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

/**
 * Configuration class for setting up LDAP (Lightweight Directory Access Protocol) integration.
 */
@Slf4j
@Configuration
public class LdapConfig {

    @Value ("${spring.ldap.urls}")
    private String ldapUrls;

    @Value ("${spring.ldap.base}")
    private String ldapBase;

    @Value ("${spring.ldap.username}")
    private String ldapUsername;

    @Value ("${spring.ldap.password}")
    private String ldapPassword;

    /**
     * Creates and configures an LdapTemplate bean.
     *
     * @return a configured LdapTemplate instance
     */
    @Bean
    public LdapTemplate ldapTemplate () {
        LdapTemplate ldapTemplate = new LdapTemplate (contextSource ());
        ldapTemplate.setIgnorePartialResultException (true);
        return ldapTemplate;
    }

    /**
     * Creates and configures an LdapContextSource bean.
     *
     * @return a configured LdapContextSource instance
     */
    @Bean
    public LdapContextSource contextSource () {
        try {
            LdapContextSource contextSource = new LdapContextSource ();
            contextSource.setUrl (ldapUrls);
            contextSource.setBase (ldapBase);
            contextSource.setUserDn (ldapUsername);
            contextSource.setPassword (ldapPassword);
            contextSource.setPooled (false);
            contextSource.setReferral ("ignore");
            log.info ("Creating LDAP context source with URLs: {}, base: {}", ldapUrls, ldapBase);
            return contextSource;
        } catch (Exception e) {
            log.error ("Failed to create LDAP context source", e);
            throw new RuntimeException (e);
        }

    }
}