package com.iclinic.iclinicbackend.modules.crm.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequestDto {

    @NotNull(message = "El ID de conversación es requerido")
    private Long conversationId;

    @NotNull(message = "El ID del usuario es requerido")
    private Long sentByUserId;

    @NotBlank(message = "El contenido del mensaje es requerido")
    private String content;
}

