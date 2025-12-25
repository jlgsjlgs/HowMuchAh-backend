package com.jlgs.howmuchah.repository;

import com.jlgs.howmuchah.entity.SettlementGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementGroupRepository extends JpaRepository<SettlementGroup, UUID> {

    // Find all settlement events for a group, ordered by most recent first
    List<SettlementGroup> findByGroupIdOrderBySettledAtDesc(UUID groupId);
}