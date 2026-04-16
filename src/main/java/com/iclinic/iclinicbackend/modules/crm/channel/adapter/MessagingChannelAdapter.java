package com.iclinic.iclinicbackend.modules.crm.channel.adapter;

import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelConnection;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;

public interface MessagingChannelAdapter {

    ChannelType supports();

    String sendText(ChannelConnection connection, String to, String message);
}

