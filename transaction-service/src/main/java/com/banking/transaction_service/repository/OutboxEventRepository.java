package com.banking.transaction_service.repository;

import com.banking.transaction_service.entity.OutboxEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

  List<OutboxEvent> findTop100ByPublishedFalseOrderByCreatedAtAsc();

}
