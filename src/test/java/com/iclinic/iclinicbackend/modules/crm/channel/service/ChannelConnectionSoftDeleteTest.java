package com.iclinic.iclinicbackend.modules.crm.channel.service;

import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.modules.company.repository.CompanyRepository;
import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelConnection;
import com.iclinic.iclinicbackend.modules.crm.channel.repository.ChannelConnectionRepository;
import com.iclinic.iclinicbackend.shared.enums.ChannelConnectionStatus;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import com.iclinic.iclinicbackend.shared.enums.ChannelProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class ChannelConnectionSoftDeleteTest {

    @Autowired
    private ChannelConnectionRepository channelConnectionRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private Company company;

    @BeforeEach
    void setUp() {
        // Crear company con constructor que tiene argumentos requeridos
        company = new com.iclinic.iclinicbackend.modules.company.entity.EcuadorianCompany(
            "Test Clinic",
            "1234567890001"
        );
        company = companyRepository.save(company);
    }

    @Test
    void testSoftDeleteMarksChannelWithDeletedAt() {
        // Setup: Create channel
        ChannelConnection channel = channelConnectionRepository.save(
            ChannelConnection.builder()
                .company(company)
                .channelType(ChannelType.TELEGRAM)
                .provider(ChannelProvider.TELEGRAM)
                .accessTokenEncrypted("encrypted_token")
                .status(ChannelConnectionStatus.ACTIVE)
                .build()
        );
        Long channelId = channel.getId();

        // Verify created without deleted_at
        ChannelConnection created = channelConnectionRepository.findById(channelId).orElseThrow();
        assertThat(created.getDeletedAt()).isNull();

        // Action: Mark as deleted
        created.setDeletedAt(LocalDateTime.now());
        channelConnectionRepository.save(created);

        // Verify marked as deleted
        ChannelConnection marked = channelConnectionRepository.findById(channelId).orElseThrow();
        assertThat(marked.getDeletedAt()).isNotNull();
    }

    @Test
    void testFindByCompanyIdExcludesDeleted() {
        // Setup: Create 2 channels, delete 1
        ChannelConnection active = channelConnectionRepository.save(
            ChannelConnection.builder()
                .company(company)
                .channelType(ChannelType.TELEGRAM)
                .provider(ChannelProvider.TELEGRAM)
                .accessTokenEncrypted("token_1")
                .status(ChannelConnectionStatus.ACTIVE)
                .build()
        );

        ChannelConnection toDelete = channelConnectionRepository.save(
            ChannelConnection.builder()
                .company(company)
                .channelType(ChannelType.WHATSAPP)
                .provider(ChannelProvider.META)
                .accessTokenEncrypted("token_2")
                .status(ChannelConnectionStatus.ACTIVE)
                .build()
        );

        // Mark one as deleted
        toDelete.setDeletedAt(LocalDateTime.now());
        channelConnectionRepository.save(toDelete);

        // Query by company
        List<ChannelConnection> results = channelConnectionRepository.findByCompanyId(company.getId());

        // Only active channel should be returned
        assertThat(results).hasSize(1)
            .extracting(ChannelConnection::getId)
            .contains(active.getId())
            .doesNotContain(toDelete.getId());
    }

    @Test
    void testFindByChannelTypeAndStatusInExcludesDeleted() {
        // Setup
        ChannelConnection active = channelConnectionRepository.save(
            ChannelConnection.builder()
                .company(company)
                .channelType(ChannelType.TELEGRAM)
                .provider(ChannelProvider.TELEGRAM)
                .accessTokenEncrypted("token_1")
                .status(ChannelConnectionStatus.ACTIVE)
                .build()
        );

        ChannelConnection deleted = channelConnectionRepository.save(
            ChannelConnection.builder()
                .company(company)
                .channelType(ChannelType.TELEGRAM)
                .provider(ChannelProvider.TELEGRAM)
                .accessTokenEncrypted("token_2")
                .status(ChannelConnectionStatus.ACTIVE)
                .build()
        );

        // Mark as deleted
        deleted.setDeletedAt(LocalDateTime.now());
        channelConnectionRepository.save(deleted);

        // Query
        List<ChannelConnection> results = channelConnectionRepository.findByChannelTypeAndStatusInWithCompany(
            ChannelType.TELEGRAM,
            java.util.Set.of(ChannelConnectionStatus.ACTIVE)
        );

        // Should only return active, non-deleted
        assertThat(results)
            .extracting(ChannelConnection::getId)
            .contains(active.getId())
            .doesNotContain(deleted.getId());
    }

    @Test
    void testDeletedChannelStillExistsInDatabase() {
        // Setup
        ChannelConnection channel = channelConnectionRepository.save(
            ChannelConnection.builder()
                .company(company)
                .channelType(ChannelType.TELEGRAM)
                .provider(ChannelProvider.TELEGRAM)
                .accessTokenEncrypted("token")
                .status(ChannelConnectionStatus.ACTIVE)
                .build()
        );
        Long channelId = channel.getId();

        // Delete (mark as deleted)
        channel.setDeletedAt(LocalDateTime.now());
        channelConnectionRepository.save(channel);

        // Direct query with findById still returns the record
        ChannelConnection found = channelConnectionRepository.findById(channelId).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getDeletedAt()).isNotNull();
    }

    @Test
    void testRecoveryOfDeletedChannel() {
        // Setup and delete
        ChannelConnection channel = channelConnectionRepository.save(
            ChannelConnection.builder()
                .company(company)
                .channelType(ChannelType.TELEGRAM)
                .provider(ChannelProvider.TELEGRAM)
                .accessTokenEncrypted("token")
                .status(ChannelConnectionStatus.ACTIVE)
                .build()
        );
        Long channelId = channel.getId();

        channel.setDeletedAt(LocalDateTime.now());
        channelConnectionRepository.save(channel);

        // Recovery: set deleted_at to null
        ChannelConnection toRecover = channelConnectionRepository.findById(channelId).orElseThrow();
        toRecover.setDeletedAt(null);
        channelConnectionRepository.save(toRecover);

        // Verify recovered
        List<ChannelConnection> recovered = channelConnectionRepository.findByCompanyId(company.getId());
        assertThat(recovered)
            .hasSize(1)
            .extracting(ChannelConnection::getId)
            .contains(channelId);
    }
}




