package com.iclinic.iclinicbackend.modules.crm.contact.service;

import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelConnection;
import com.iclinic.iclinicbackend.modules.crm.contact.entity.CrmContact;
import com.iclinic.iclinicbackend.modules.crm.webhook.dto.IncomingChannelMessage;

public interface CrmContactService {

    /**
     * Resuelve (o crea) el contacto CRM que corresponde a un mensaje entrante.
     * <p>
     * Prioridad de búsqueda:
     * <ol>
     *   <li>ChannelUserLink por (channelType, externalUserId) → devuelve el contacto vinculado,
     *       actualiza displayName/username si cambió.</li>
     *   <li>CrmContactPhone por (normalizedPhone, companyId) → vincula el canal al
     *       contacto existente con ese teléfono y devuelve el contacto.</li>
     *   <li>Crea un nuevo CrmContact, CrmContactPhone (si hay teléfono) y ChannelUserLink.</li>
     * </ol>
     *
     * @param connection Conexión de canal activa (provee branch y company).
     * @param msg        Mensaje entrante normalizado.
     * @return Contacto CRM resuelto o recién creado.
     */
    CrmContact resolveContact(ChannelConnection connection, IncomingChannelMessage msg);

    /** @deprecated Usar {@link #resolveContact(ChannelConnection, IncomingChannelMessage)} */
    @Deprecated
    CrmContact findOrCreate(Long companyId, Long branchId, String fullName, String phone);
}
