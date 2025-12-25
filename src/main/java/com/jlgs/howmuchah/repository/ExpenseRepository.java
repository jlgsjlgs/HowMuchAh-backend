package com.jlgs.howmuchah.repository;

import com.jlgs.howmuchah.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    // Get all expenses for a group (with pagination)
    Page<Expense> findByGroupId(UUID groupId, Pageable pageable);

    // Mark all pending expenses as settled
    @Modifying
    @Query("UPDATE Expense e SET e.isSettled = true " +
            "WHERE e.group.id = :groupId AND e.isSettled = false")
    void markAllAsSettledByGroupId(@Param("groupId") UUID groupId);

    // Get number of unsettled expenses for a group
    Long countByGroupIdAndIsSettled(UUID groupId, boolean isSettled);
}