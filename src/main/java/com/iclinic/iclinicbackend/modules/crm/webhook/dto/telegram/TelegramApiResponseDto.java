package com.iclinic.iclinicbackend.modules.crm.webhook.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramApiResponseDto {

    @JsonProperty("ok")
    private Boolean ok;

    @JsonProperty("result")
    private TelegramApiResultDto result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TelegramApiResultDto {
        @JsonProperty("message_id")
        private Long messageId;

        @JsonProperty("username")
        private String username;

        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("is_bot")
        private Boolean isBot;
    }
}
