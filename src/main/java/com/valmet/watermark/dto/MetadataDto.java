package com.valmet.watermark.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MetadataDto {
    private String created_time;
    private Object custom_metadata;
    private String deletion_time;
    private Boolean destroyed;
    private Integer version;
}