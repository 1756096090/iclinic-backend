package com.iclinic.iclinicbackend.modules.crm.channel.repository;

import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelConnection;
import com.iclinic.iclinicbackend.shared.enums.ChannelConnectionStatus;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChannelConnectionRepository extends JpaRepository<ChannelConnection, Long> {

    @Query("SELECT c FROM ChannelConnection c JOIN FETCH c.company " +
           "WHERE c.channelType = :channelType AND c.status IN :statuses AND c.deletedAt IS NULL")
    List<ChannelConnection> findByChannelTypeAndStatusInWithCompany(
            @Param("channelType") ChannelType channelType,
            @Param("statuses") Collection<ChannelConnectionStatus> statuses
    );

    /**
     * Busca una conexión activa o verificada para la empresa y canal dados.
     * Se aceptan ambos estados porque el ciclo de vida es: PENDING → VERIFIED → ACTIVE,
     * y las conexiones verificadas ya pueden recibir mensajes.
     */
    @Query("SELECT c FROM ChannelConnection c " +
           "WHERE c.company.id = :companyId " +
           "AND c.channelType = :channelType " +
           "AND c.status IN :statuses " +
           "AND c.deletedAt IS NULL")
    Optional<ChannelConnection> findActiveByCompanyAndChannel(
            @Param("companyId") Long companyId,
            @Param("channelType") ChannelType channelType,
            @Param("statuses") Collection<ChannelConnectionStatus> statuses
    );

    @Query("SELECT c FROM ChannelConnection c " +
           "WHERE c.company.id = :companyId " +
           "AND c.channelType = :channelType " +
           "AND c.status = :status " +
           "AND c.deletedAt IS NULL")
    Optional<ChannelConnection> findByCompanyIdAndChannelTypeAndStatus(
            Long companyId,
            ChannelType channelType,
            ChannelConnectionStatus status
    );

    @Query("SELECT c FROM ChannelConnection c " +
           "WHERE c.webhookVerifyToken = :token AND c.deletedAt IS NULL")
    Optional<ChannelConnection> findByWebhookVerifyToken(@Param("token") String webhookVerifyToken);

    /**
     * Busca una conexión operativa de WhatsApp por externalPhoneNumberId cargando la empresa.
     * Usado en Meta webhooks para resolver companyId sin confiar en el payload.
     */
    @Query("SELECT c FROM ChannelConnection c JOIN FETCH c.company " +
           "WHERE c.externalPhoneNumberId = :phoneNumberId " +
           "AND c.channelType = :channelType " +
           "AND c.status IN :statuses " +
           "AND c.deletedAt IS NULL")
    Optional<ChannelConnection> findByExternalPhoneNumberIdAndChannelTypeAndStatusInWithCompany(
            @Param("phoneNumberId") String phoneNumberId,
            @Param("channelType") ChannelType channelType,
            @Param("statuses") Collection<ChannelConnectionStatus> statuses
    );

    @Query("SELECT c FROM ChannelConnection c WHERE c.company.id = :companyId AND c.deletedAt IS NULL")
    List<ChannelConnection> findByCompanyId(@Param("companyId") Long companyId);
}
