package com.iclinic.iclinicbackend.modules.crm.channel.dto;

import com.iclinic.iclinicbackend.shared.enums.ChannelProvider;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateChannelConnectionRequestDto {

    @NotNull(message = "El ID de empresa es requerido")
    private Long companyId;

    private Long branchId;

    @NotNull(message = "El tipo de canal es requerido")
    private ChannelType channelType;

    @NotNull(message = "El proveedor es requerido")
    private ChannelProvider provider;

    private String externalAccountId;

    private String externalPhoneNumberId;

    @NotBlank(message = "El token de acceso es requerido")
    private String accessToken;

    @NotBlank(message = "El token de verificación del webhook es requerido")
    private String webhookVerifyToken;

    private String webhookBaseUrl;
}

