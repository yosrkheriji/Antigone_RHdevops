package com.antigone.rh.ai.repository;

import com.antigone.rh.ai.entity.AiToolAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiToolAuditRepository extends JpaRepository<AiToolAuditLog, Long> {

    List<AiToolAuditLog> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
