package com.jlgs.howmuchah.service;

import com.jlgs.howmuchah.dto.response.SettlementDetailResponse;
import com.jlgs.howmuchah.dto.response.SettlementSummaryResponse;
import com.jlgs.howmuchah.entity.*;
import com.jlgs.howmuchah.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementGroupRepository settlementGroupRepository;
    private final SettlementRepository settlementRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final ExpenseRepository expenseRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<SettlementSummaryResponse> getSettlementHistory(UUID groupId) {
        // Validate group exists
        if (!groupRepository.existsById(groupId)) {
            throw new IllegalArgumentException("Group not found");
        }

        List<SettlementGroup> settlementGroups =
                settlementGroupRepository.findByGroupIdOrderBySettledAtDesc(groupId);

        return settlementGroups.stream()
                .map(SettlementSummaryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SettlementDetailResponse getSettlementDetail(UUID settlementGroupId) {
        SettlementGroup settlementGroup = settlementGroupRepository.findById(settlementGroupId)
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found"));

        return SettlementDetailResponse.from(settlementGroup);
    }

    /**
     * Execute settlement for a group
     * 1. Get all unsettled expenses
     * 2. Group by currency
     * 3. Calculate settlements per currency
     * 4. Save settlements
     * 5. Mark expenses as settled
     */
    @Transactional
    public SettlementDetailResponse executeSettlement(UUID groupId) {
        // 1. Validate group exists - Uses a lock to prevent race condition (two people settle at once)
        Group group = groupRepository.findByIdWithLock(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // 2. Get all unsettled expense splits for this group
        List<ExpenseSplit> unsettledSplits = expenseSplitRepository.findUnsettledByGroupId(groupId);

        if (unsettledSplits.isEmpty()) {
            throw new IllegalArgumentException("No unsettled expenses to settle");
        }

        // 3. Group splits by currency
        Map<String, List<ExpenseSplit>> splitsByCurrency = unsettledSplits.stream()
                .collect(Collectors.groupingBy(split -> split.getExpense().getCurrency()));

        // 4. Create settlement group (parent record)
        SettlementGroup settlementGroup = SettlementGroup.builder()
                .group(group)
                .build();
        settlementGroup = settlementGroupRepository.save(settlementGroup);

        // 5. Calculate settlements per currency
        List<Settlement> allSettlements = new ArrayList<>();

        for (Map.Entry<String, List<ExpenseSplit>> entry : splitsByCurrency.entrySet()) {
            String currency = entry.getKey();
            List<ExpenseSplit> currencySplits = entry.getValue();

            // Calculate net balances for this currency
            Map<UUID, BigDecimal> balances = calculateBalances(currencySplits);

            // Run Splitwise algorithm to minimize transactions
            List<Settlement> currencySettlements = minimizeTransactions(
                    balances,
                    currency,
                    settlementGroup
            );

            allSettlements.addAll(currencySettlements);
        }

        if (allSettlements.isEmpty()) {
            log.info("Settlement created with zero transactions (perfect wash) for group {}", groupId);
        }

        // 6. Save all settlement transactions
        if (!allSettlements.isEmpty()) {
            settlementRepository.saveAll(allSettlements);
        }

        // 7. Mark all expense splits and expenses as settled
        expenseSplitRepository.markAllAsSettledByGroupId(groupId);
        expenseRepository.markAllAsSettledByGroupId(groupId);

        // 8. Reload settlement group with transactions and return
        settlementGroup = settlementGroupRepository.findById(settlementGroup.getId())
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found"));

        return SettlementDetailResponse.from(settlementGroup);
    }

    /**
     * Calculate net balance for each user in a specific currency
     * Net balance = (total paid) - (total owed)
     * Positive = creditor (owed money)
     * Negative = debtor (owes money)
     */
    private Map<UUID, BigDecimal> calculateBalances(List<ExpenseSplit> splits) {
        Map<UUID, BigDecimal> balances = new HashMap<>();

        // Group splits by expense to process each expense once
        Map<Expense, List<ExpenseSplit>> splitsByExpense = splits.stream()
                .collect(Collectors.groupingBy(ExpenseSplit::getExpense));

        for (Map.Entry<Expense, List<ExpenseSplit>> entry : splitsByExpense.entrySet()) {
            Expense expense = entry.getKey();
            List<ExpenseSplit> expenseSplits = entry.getValue();
            UUID payerId = expense.getPaidBy().getId();

            BigDecimal sumOfSplits = BigDecimal.ZERO;

            for (ExpenseSplit split : expenseSplits) {
                UUID debtorId = split.getUser().getId();
                BigDecimal amount = split.getAmountOwed();

                // Debit the debtor
                balances.merge(debtorId, amount.negate(), BigDecimal::add);

                sumOfSplits = sumOfSplits.add(amount);
            }

            // Solves phantom penny problem
            balances.merge(payerId, sumOfSplits, BigDecimal::add);
        }

        return balances;
    }

    /**
     * Splitwise greedy algorithm to minimize number of transactions
     *
     * Algorithm:
     * 1. Separate users into creditors (positive balance) and debtors (negative balance)
     * 2. Sort creditors descending (largest first)
     * 3. Sort debtors ascending (most negative first)
     * 4. Match largest creditor with largest debtor
     * 5. Create transaction for min(creditor_balance, abs(debtor_balance))
     * 6. Update balances and repeat until all settled
     */
    private List<Settlement> minimizeTransactions(
            Map<UUID, BigDecimal> balances,
            String currency,
            SettlementGroup settlementGroup) {

        List<Settlement> settlements = new ArrayList<>();

        // Fetch users at the start to prevent N+1 query problem
        Set<UUID> userIds = balances.keySet();
        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // Separate creditors and debtors
        List<UserBalance> creditors = new ArrayList<>();
        List<UserBalance> debtors = new ArrayList<>();

        // Use threshold to handle rounding errors - Solves phantom penny problem
        BigDecimal threshold = new BigDecimal("0.005");

        for (Map.Entry<UUID, BigDecimal> entry : balances.entrySet()) {
            BigDecimal balance = entry.getValue();

            // Skip near-zero balances
            if (balance.abs().compareTo(threshold) < 0) {
                continue;
            }

            if (balance.signum() > 0) {
                creditors.add(new UserBalance(entry.getKey(), balance));
            } else {
                debtors.add(new UserBalance(entry.getKey(), balance.abs()));
            }
        }

        // Sort descending
        creditors.sort((a, b) -> b.balance.compareTo(a.balance));
        debtors.sort((a, b) -> b.balance.compareTo(a.balance));

        int i = 0;
        int j = 0;

        while (i < creditors.size() && j < debtors.size()) {
            UserBalance creditor = creditors.get(i);
            UserBalance debtor = debtors.get(j);

            BigDecimal amount = creditor.balance.min(debtor.balance)
                    .setScale(2, RoundingMode.HALF_UP);

            Settlement settlement = Settlement.builder()
                    .payer(userMap.get(debtor.userId))
                    .payee(userMap.get(creditor.userId))
                    .amount(amount)
                    .currency(currency)
                    .settlementGroup(settlementGroup)
                    .build();

            settlements.add(settlement);

            creditor.balance = creditor.balance.subtract(amount);
            debtor.balance = debtor.balance.subtract(amount);

            if (creditor.balance.abs().compareTo(threshold) < 0) {
                i++;
            }
            if (debtor.balance.abs().compareTo(threshold) < 0) {
                j++;
            }
        }

        return settlements;
    }

    /**
     * Helper class to track user balances during algorithm
     */
    private static class UserBalance {
        UUID userId;
        BigDecimal balance;

        UserBalance(UUID userId, BigDecimal balance) {
            this.userId = userId;
            this.balance = balance;
        }
    }
}