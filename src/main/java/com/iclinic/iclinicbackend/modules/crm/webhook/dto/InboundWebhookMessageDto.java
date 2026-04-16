package com.iclinic.iclinicbackend.modules.crm.webhook.dto;

import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InboundWebhookMessageDto {
    private ChannelType channelType;
    private Long companyId;
    private String contactName;
    private String contactPhone;
    private String externalMessageId;
    private String content;
}

