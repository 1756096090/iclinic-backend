package com.iclinic.iclinicbackend.modules.crm.channel.adapter;

import com.iclinic.iclinicbackend.modules.crm.exception.InvalidChannelConfigurationException;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MessagingChannelAdapterRegistry {

    private final List<MessagingChannelAdapter> adapters;

    public MessagingChannelAdapterRegistry(List<MessagingChannelAdapter> adapters) {
        this.adapters = adapters;
    }

    public MessagingChannelAdapter get(ChannelType channelType) {
        return adapters.stream()
                .filter(a -> a.supports() == channelType)
                .findFirst()
                .orElseThrow(() -> new InvalidChannelConfigurationException("No existe adapter para: " + channelType));
    }
}
