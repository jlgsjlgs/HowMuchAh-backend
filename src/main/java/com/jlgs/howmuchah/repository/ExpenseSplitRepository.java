package com.jlgs.howmuchah.repository;

import com.jlgs.howmuchah.entity.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, UUID> {

    // Get all splits for an expense
    List<ExpenseSplit> findByExpenseId(UUID expenseId);

    // Delete all splits for an expense (Used when updating expense splits)
    @Modifying
    @Query("DELETE FROM ExpenseSplit es WHERE es.expense.id = :expenseId")
    void deleteByExpenseId(@Param("expenseId") UUID expenseId);

    // Find all unsettled splits for an expense group
    @Query("SELECT es FROM ExpenseSplit es " +
            "JOIN es.expense e " +
            "WHERE e.group.id = :groupId AND es.isSettled = false")
    List<ExpenseSplit> findUnsettledByGroupId(@Param("groupId") UUID groupId);

    // Mark all pending expenses as settled
    @Modifying
    @Query("UPDATE ExpenseSplit es SET es.isSettled = true " +
            "WHERE es.expense.group.id = :groupId AND es.isSettled = false")
    void markAllAsSettledByGroupId(@Param("groupId") UUID groupId);
}