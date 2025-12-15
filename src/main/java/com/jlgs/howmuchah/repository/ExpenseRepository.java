package com.jlgs.howmuchah.repository;

import com.jlgs.howmuchah.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    // Get all expenses for a group (with pagination)
    Page<Expense> findByGroupId(UUID groupId, Pageable pageable);

    // Check if user has any expenses
    boolean existsByPaidById(UUID userId);
}