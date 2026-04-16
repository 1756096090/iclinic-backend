package com.iclinic.iclinicbackend.modules.crm.webhook.dto.telegram;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendTelegramMessageRequestDto {
    private String chatId;
    private String text;
}
