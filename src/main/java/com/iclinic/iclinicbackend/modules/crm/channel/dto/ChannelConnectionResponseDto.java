package com.iclinic.iclinicbackend.modules.crm.channel.dto;

import com.iclinic.iclinicbackend.shared.enums.ChannelConnectionStatus;
import com.iclinic.iclinicbackend.shared.enums.ChannelProvider;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChannelConnectionResponseDto {
    private Long id;
    private Long branchId;
    private ChannelType channelType;
    private ChannelProvider provider;
    private String externalAccountId;
    private String externalPhoneNumberId;
    private ChannelConnectionStatus status;
    private LocalDateTime createdAt;
}

