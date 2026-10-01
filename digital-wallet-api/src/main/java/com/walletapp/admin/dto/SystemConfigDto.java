package com.walletapp.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigDto {

    private UUID id;

    @JsonProperty("config_key")
    private String configKey;

    @JsonProperty("config_value")
    private String configValue;

    private String description;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
}
