package com.iclinic.iclinicbackend.modules.crm.webhook.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** Ubicación geográfica en un mensaje de Telegram. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramLocationDto {

    @JsonProperty("longitude")
    private Double longitude;

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("horizontal_accuracy")
    private Double horizontalAccuracy;
}

