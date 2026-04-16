package com.iclinic.iclinicbackend.modules.crm.conversation.repository;

import com.iclinic.iclinicbackend.modules.crm.conversation.entity.Conversation;
import com.iclinic.iclinicbackend.shared.enums.ConversationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findFirstByContactIdAndStatusOrderByCreatedAtDesc(
            Long contactId,
            ConversationStatus status
    );

    List<Conversation> findByContactCompanyIdAndStatusOrderByLastMessageAtDesc(
            Long companyId,
            ConversationStatus status
    );

    List<Conversation> findByContactBranchIdOrderByLastMessageAtDesc(Long branchId);

    List<Conversation> findByContactCompanyIdOrderByLastMessageAtDesc(Long companyId);
}

