package com.jlgs.howmuchah.repository;

import com.jlgs.howmuchah.entity.SettlementGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettlementGroupRepository extends JpaRepository<SettlementGroup, UUID> {

    // Find all settlement events for a group, ordered by most recent first
    @Query("SELECT sg FROM SettlementGroup sg " +
            "LEFT JOIN FETCH sg.settlements " +
            "WHERE sg.group.id = :groupId " +
            "ORDER BY sg.settledAt DESC")
    List<SettlementGroup> findByGroupIdOrderBySettledAtDesc(@Param("groupId") UUID groupId);
}