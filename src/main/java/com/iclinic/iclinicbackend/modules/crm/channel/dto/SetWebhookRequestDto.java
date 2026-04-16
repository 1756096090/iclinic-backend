package com.iclinic.iclinicbackend.modules.crm.channel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SetWebhookRequestDto {

    @NotBlank(message = "La URL base del webhook es requerida")
    private String webhookBaseUrl;
}

