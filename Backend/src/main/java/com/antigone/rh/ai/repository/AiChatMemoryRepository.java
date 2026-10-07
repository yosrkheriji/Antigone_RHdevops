package com.antigone.rh.ai.repository;

import com.antigone.rh.ai.entity.AiChatMemoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiChatMemoryRepository extends JpaRepository<AiChatMemoryRecord, Long> {
}
