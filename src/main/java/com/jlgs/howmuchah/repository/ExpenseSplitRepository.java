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

    // Check if user has any expenses splits
    boolean existsByUserId(UUID userId);
}