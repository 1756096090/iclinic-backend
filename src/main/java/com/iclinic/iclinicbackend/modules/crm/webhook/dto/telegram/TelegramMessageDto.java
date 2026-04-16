package com.iclinic.iclinicbackend.modules.crm.webhook.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramMessageDto {

    @JsonProperty("message_id")
    private Long messageId;

    @JsonProperty("from")
    private TelegramUserDto from;

    @JsonProperty("chat")
    private TelegramChatDto chat;

    @JsonProperty("date")
    private Long date;

    @JsonProperty("text")
    private String text;

    @JsonProperty("caption")
    private String caption;

    @JsonProperty("photo")
    private List<TelegramPhotoSizeDto> photo;

    @JsonProperty("sticker")
    private TelegramStickerDto sticker;

    @JsonProperty("location")
    private TelegramLocationDto location;

    @JsonProperty("contact")
    private TelegramContactDto contact;

    @JsonProperty("reply_to_message")
    private TelegramMessageDto replyToMessage;

    public String resolveText() {
        if (text != null && !text.isBlank()) return text;
        if (caption != null && !caption.isBlank()) return caption;
        return null;
    }

    public boolean hasContent() {
        return resolveText() != null || photo != null || sticker != null
                || location != null || contact != null;
    }
}
