package com.banking.transaction_service.repository;

import com.banking.transaction_service.entity.DeadLetterEvent;
import com.banking.transaction_service.enums.DeadLetterStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, UUID> {

  Page<DeadLetterEvent> findByStatus(DeadLetterStatus status, Pageable pageable);

  boolean existsByOriginalTopicAndOriginalPartitionAndOriginalOffset(String originalTopic,Integer originalPartition,Long originalOffset);
}