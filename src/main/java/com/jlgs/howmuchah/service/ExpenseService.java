package com.jlgs.howmuchah.service;

import com.jlgs.howmuchah.dto.request.ExpenseCreationRequest;
import com.jlgs.howmuchah.dto.request.ExpenseSplitDto;
import com.jlgs.howmuchah.dto.request.ExpenseUpdateRequest;
import com.jlgs.howmuchah.dto.response.ExpenseDetailResponse;
import com.jlgs.howmuchah.dto.response.ExpenseResponse;
import com.jlgs.howmuchah.entity.Expense;
import com.jlgs.howmuchah.entity.ExpenseSplit;
import com.jlgs.howmuchah.entity.Group;
import com.jlgs.howmuchah.entity.User;
import com.jlgs.howmuchah.repository.ExpenseRepository;
import com.jlgs.howmuchah.repository.ExpenseSplitRepository;
import com.jlgs.howmuchah.repository.GroupMemberRepository;
import com.jlgs.howmuchah.repository.GroupRepository;
import com.jlgs.howmuchah.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public ExpenseDetailResponse createExpense(ExpenseCreationRequest request) {
        // Check if group exists
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Check if user is part of the group
        if (!groupMemberRepository.existsByGroupIdAndUserId(request.getGroupId(), request.getPaidByUserId())) {
            throw new IllegalArgumentException("Payer must be a current group member");
        }

        Set<UUID> splitUserIds = request.getSplits().stream()
                .map(ExpenseSplitDto::getUserId)
                .collect(Collectors.toSet());

        Set<UUID> memberIds = groupMemberRepository.findByGroupId(request.getGroupId())
                .stream()
                .map(gm -> gm.getUser().getId())
                .collect(Collectors.toSet());

        // Check if all participants are group members
        if (!memberIds.containsAll(splitUserIds)) {
            throw new IllegalArgumentException("All expense participants must be current group members");
        }

        // Ensure split adds up to expense total
        validateSplitAmounts(request.getTotalAmount(), request.getSplits());

        User paidBy = userRepository.findById(request.getPaidByUserId())
                .orElseThrow(() -> new RuntimeException("Payer user not found"));

        Expense expense = Expense.builder()
                .group(group)
                .description(request.getDescription())
                .totalAmount(request.getTotalAmount())
                .currency(request.getCurrency())
                .paidBy(paidBy)
                .category(request.getCategory())
                .expenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : LocalDate.now())
                .build();

        expense = expenseRepository.save(expense);

        // Generate splits
        List<ExpenseSplit> splits = createExpenseSplits(expense, request.getSplits());

        return ExpenseDetailResponse.from(expense, splits);
    }

    @Transactional
    public ExpenseDetailResponse updateExpense(UUID expenseId, ExpenseUpdateRequest request) {
        // Check if expense exists
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        // Block updates for already settled expenses
        if (expense.isSettled()) {
            throw new IllegalArgumentException("Cannot modify settled expense.");
        }

        // Check if user is part of the group
        UUID groupId = expense.getGroup().getId();
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, request.getPaidByUserId())) {
            throw new IllegalArgumentException("Payer must be a current group member");
        }

        // Check if all participants are group members
        Set<UUID> splitUserIds = request.getSplits().stream()
                .map(ExpenseSplitDto::getUserId)
                .collect(Collectors.toSet());

        Set<UUID> memberIds = groupMemberRepository.findByGroupId(groupId)
                .stream()
                .map(gm -> gm.getUser().getId())
                .collect(Collectors.toSet());

        if (!memberIds.containsAll(splitUserIds)) {
            throw new IllegalArgumentException("All expense participants must be current group members");
        }

        // Ensure split adds up to expense total
        validateSplitAmounts(request.getTotalAmount(), request.getSplits());

        User paidBy = userRepository.findById(request.getPaidByUserId())
                .orElseThrow(() -> new RuntimeException("Payer user not found"));

        expense.setDescription(request.getDescription());
        expense.setTotalAmount(request.getTotalAmount());
        expense.setCurrency(request.getCurrency());
        expense.setPaidBy(paidBy);
        expense.setCategory(request.getCategory());
        expense.setExpenseDate(request.getExpenseDate());

        expense = expenseRepository.save(expense);

        // Delete all old splits for this expense, and regenerate new ones
        expenseSplitRepository.deleteByExpenseId(expenseId);
        List<ExpenseSplit> splits = createExpenseSplits(expense, request.getSplits());

        return ExpenseDetailResponse.from(expense, splits);
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpensesByGroup(UUID groupId, Pageable pageable) {
        if (!groupRepository.existsById(groupId)) {
            throw new RuntimeException("Group not found");
        }

        Page<Expense> expenses = expenseRepository.findByGroupId(groupId, pageable);

        return expenses.map(ExpenseResponse::from);
    }

    @Transactional
    public void deleteExpense(UUID expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (expense.isSettled()) {
            throw new IllegalArgumentException("Cannot delete settled expense.");
        }

        expenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public ExpenseDetailResponse getExpenseDetail(UUID expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        List<ExpenseSplit> splits = expenseSplitRepository.findByExpenseId(expenseId);

        return ExpenseDetailResponse.from(expense, splits);
    }

    /**
     * Helper functions
     */

    private void validateSplitAmounts(BigDecimal totalAmount, List<ExpenseSplitDto> splits) {
        BigDecimal splitSum = splits.stream()
                .map(ExpenseSplitDto::getAmountOwed)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal difference = totalAmount.subtract(splitSum).abs();
        BigDecimal tolerance = new BigDecimal("0.01");

        if (difference.compareTo(tolerance) > 0) {
            throw new IllegalArgumentException(
                    String.format("Split amounts (%.2f) must equal total amount (%.2f)",
                            splitSum, totalAmount)
            );
        }

        if (splits.isEmpty()) {
            throw new IllegalArgumentException("Expense must have at least one split");
        }
    }

    private List<ExpenseSplit> createExpenseSplits(Expense expense, List<ExpenseSplitDto> splitDtos) {
        List<ExpenseSplit> splits = splitDtos.stream()
                .map(splitDto -> {
                    User user = userRepository.findById(splitDto.getUserId())
                            .orElseThrow(() -> new RuntimeException("User not found: " + splitDto.getUserId()));

                    return ExpenseSplit.builder()
                            .expense(expense)
                            .user(user)
                            .amountOwed(splitDto.getAmountOwed())
                            .isSettled(false)
                            .build();
                })
                .collect(Collectors.toList());

        return expenseSplitRepository.saveAll(splits);
    }
}