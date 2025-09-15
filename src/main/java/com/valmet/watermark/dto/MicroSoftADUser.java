package com.valmet.watermark.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MicroSoftADUser {
    private String username;       // sAMAccountName
    private String upn;            // userPrincipalName
    private String displayName;
    private String email;
    private List<String> groups;   // Group CNs
    private String dn;             // Full Distinguished Name
}