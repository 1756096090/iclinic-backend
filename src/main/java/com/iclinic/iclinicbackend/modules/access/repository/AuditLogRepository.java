package com.iclinic.iclinicbackend.modules.access.repository;

import com.iclinic.iclinicbackend.modules.access.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByCompanyIdOrderByCreatedAtDesc(Long companyId, Pageable pageable);
    Page<AuditLog> findByActorUserIdOrderByCreatedAtDesc(Long actorUserId, Pageable pageable);
}
