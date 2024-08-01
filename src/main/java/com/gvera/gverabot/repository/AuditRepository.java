package com.gvera.gverabot.repository;

import com.gvera.gverabot.entity.Item;
import com.gvera.gverabot.entity.ItemAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditRepository extends JpaRepository<ItemAudit, UUID> {
    List<ItemAudit> findAllByUpdatedAtBetween(LocalDate startDate, LocalDate finishDate);

    Optional<ItemAudit> findByItem(Item item);
}
