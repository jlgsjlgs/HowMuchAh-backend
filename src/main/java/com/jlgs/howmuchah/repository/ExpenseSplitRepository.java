package com.jlgs.howmuchah.repository;

import com.jlgs.howmuchah.entity.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, UUID> {

    // Get all splits for an expense
    List<ExpenseSplit> findByExpenseId(UUID expenseId);

    // Delete all splits for an expense (Used when updating expense splits)
    void deleteByExpenseId(UUID expenseId);

    // Check if all splits for an expense is settled (to prevent modification to expense after settling)
    boolean existsByExpenseIdAndIsSettledTrue(UUID expenseId);

    // Check if user has any expenses splits
    boolean existsByUserId(UUID userId);
}