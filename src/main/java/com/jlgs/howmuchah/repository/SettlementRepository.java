package com.jlgs.howmuchah.repository;

import com.jlgs.howmuchah.entity.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    // Get all settlements for a group (with pagination)
    Page<Settlement> findByGroupId(UUID groupId, Pageable pageable);

    // Check if user has any settlements
    boolean existsByPayerId(UUID userId);
    boolean existsByPayeeId(UUID userId);
}