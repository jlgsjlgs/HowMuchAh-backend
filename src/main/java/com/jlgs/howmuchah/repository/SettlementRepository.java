package com.jlgs.howmuchah.repository;

import com.jlgs.howmuchah.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    // Find all transactions for a specific settlement event
    List<Settlement> findBySettlementGroupId(UUID settlementGroupId);
}