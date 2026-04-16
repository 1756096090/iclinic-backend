package com.iclinic.iclinicbackend.modules.crm.channel.service;

import com.iclinic.iclinicbackend.modules.crm.channel.dto.ChannelConnectionResponseDto;
import com.iclinic.iclinicbackend.modules.crm.channel.dto.CreateChannelConnectionRequestDto;
import com.iclinic.iclinicbackend.modules.crm.channel.dto.UpdateChannelConnectionRequestDto;
import com.iclinic.iclinicbackend.modules.crm.channel.dto.WebhookInfoDto;

import java.util.List;

public interface ChannelConnectionService {
    List<ChannelConnectionResponseDto> getAll();
    ChannelConnectionResponseDto create(CreateChannelConnectionRequestDto dto);
    ChannelConnectionResponseDto update(Long id, UpdateChannelConnectionRequestDto dto);
    ChannelConnectionResponseDto activate(Long id);
    ChannelConnectionResponseDto deactivate(Long id);
    List<ChannelConnectionResponseDto> findByCompany(Long companyId);
    void deleteById(Long id);
    ChannelConnectionResponseDto setWebhook(Long id, String webhookBaseUrl);
    WebhookInfoDto getWebhookInfo(Long id);
}

