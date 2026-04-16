package com.iclinic.iclinicbackend.modules.crm.webhook.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramUpdateDto {

    @JsonProperty("update_id")
    private Long updateId;

    @JsonProperty("message")
    private TelegramMessageDto message;

    @JsonProperty("edited_message")
    private TelegramMessageDto editedMessage;

    @JsonProperty("channel_post")
    private TelegramMessageDto channelPost;

    @JsonProperty("edited_channel_post")
    private TelegramMessageDto editedChannelPost;

    @JsonProperty("callback_query")
    private TelegramCallbackQueryDto callbackQuery;

    public TelegramMessageDto resolveMessage() {
        if (message != null) return message;
        if (editedMessage != null) return editedMessage;
        if (channelPost != null) return channelPost;
        if (editedChannelPost != null) return editedChannelPost;
        if (callbackQuery != null) return callbackQuery.getMessage();
        return null;
    }

    public boolean isEdited() {
        return editedMessage != null || editedChannelPost != null;
    }
}
