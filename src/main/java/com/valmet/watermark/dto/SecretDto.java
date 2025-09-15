package com.valmet.watermark.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Builder
@Data
public class SecretDto {
    private Map<String, String> data;
    private MetadataDto metadata;
}
