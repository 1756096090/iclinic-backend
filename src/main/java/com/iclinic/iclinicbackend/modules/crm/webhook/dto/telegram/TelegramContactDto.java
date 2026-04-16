package com.iclinic.iclinicbackend.modules.crm.webhook.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** Contacto compartido en un mensaje de Telegram. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramContactDto {

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("user_id")
    private Long userId;
}

