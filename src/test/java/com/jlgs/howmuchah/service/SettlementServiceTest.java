package com.jlgs.howmuchah.service;

import com.jlgs.howmuchah.dto.response.SettlementDetailResponse;
import com.jlgs.howmuchah.dto.response.SettlementSummaryResponse;
import com.jlgs.howmuchah.entity.*;
import com.jlgs.howmuchah.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementService Unit Tests")
class SettlementServiceTest {

    @Mock
    private SettlementGroupRepository settlementGroupRepository;
    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private ExpenseSplitRepository expenseSplitRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private SettlementService settlementService;

    @Captor
    private ArgumentCaptor<List<Settlement>> settlementListCaptor;

    private UUID requesterId;
    private UUID groupId;
    private User userA;
    private User userB;
    private User userC;
    private Group testGroup;
    private SettlementGroup testSettlementGroup;

    @BeforeEach
    void setUp() {
        requesterId = UUID.randomUUID();
        groupId = UUID.randomUUID();

        userA = createUser("User A");
        userB = createUser("User B");
        userC = createUser("User C");

        testGroup = Group.builder()
                .id(groupId)
                .name("Test Group")
                .build();

        testSettlementGroup = SettlementGroup.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .settlements(new ArrayList<>())
                .build();
    }

    // ==================== executeSettlement Tests ====================

    @Test
    @DisplayName("executeSettlement - Should optimize Cyclic Debt (A->B->C->A) to zero transactions")
    void executeSettlement_CyclicDebt_ShouldResultInNoTransactions() {
        // Arrange
        Expense exp1 = createExpense(userA, "30.00");
        ExpenseSplit split1 = createSplit(exp1, userB, "30.00");

        Expense exp2 = createExpense(userB, "30.00");
        ExpenseSplit split2 = createSplit(exp2, userC, "30.00");

        Expense exp3 = createExpense(userC, "30.00");
        ExpenseSplit split3 = createSplit(exp3, userA, "30.00");

        List<ExpenseSplit> allSplits = List.of(split1, split2, split3);

        setupMocksForSettlement(allSplits);

        // Act
        SettlementDetailResponse response = settlementService.executeSettlement(requesterId, groupId);

        // Assert
        verify(settlementRepository, never()).saveAll(any());
        verify(expenseSplitRepository).markAllAsSettledByGroupId(groupId);
        verify(expenseRepository).markAllAsSettledByGroupId(groupId);
        assertThat(response).isNotNull();
        assertThat(response.getTransactions()).isEmpty();
    }

    @Test
    @DisplayName("executeSettlement - Should handle Phantom Penny (Total 100 vs Splits 99.99)")
    void executeSettlement_PhantomPenny_ShouldHandleRoundingCorrectly() {
        // Arrange
        Expense expense = createExpense(userA, "100.00");
        ExpenseSplit splitA = createSplit(expense, userA, "33.33");
        ExpenseSplit splitB = createSplit(expense, userB, "33.33");
        ExpenseSplit splitC = createSplit(expense, userC, "33.33");

        List<ExpenseSplit> allSplits = List.of(splitA, splitB, splitC);

        setupMocksForSettlement(allSplits);

        // Act
        settlementService.executeSettlement(requesterId, groupId);

        // Assert
        verify(settlementRepository).saveAll(settlementListCaptor.capture());
        List<Settlement> savedSettlements = settlementListCaptor.getValue();

        assertThat(savedSettlements).hasSize(2);
        assertThat(savedSettlements)
                .allSatisfy(s -> assertThat(s.getAmount()).isEqualByComparingTo("33.33"));
        assertThat(savedSettlements)
                .allMatch(s -> s.getPayee().getId().equals(userA.getId()));
    }

    @Test
    @DisplayName("executeSettlement - Should simplify complex debts correctly (Greedy Algorithm)")
    void executeSettlement_ComplexScenario_ShouldMinimizeTransactions() {
        // Arrange
        Expense exp1 = createExpense(userA, "100.00");
        ExpenseSplit split1 = createSplit(exp1, userB, "100.00");

        Expense exp2 = createExpense(userB, "50.00");
        ExpenseSplit split2 = createSplit(exp2, userC, "50.00");

        List<ExpenseSplit> allSplits = List.of(split1, split2);

        setupMocksForSettlement(allSplits);

        // Act
        settlementService.executeSettlement(requesterId, groupId);

        // Assert
        verify(settlementRepository).saveAll(settlementListCaptor.capture());
        List<Settlement> results = settlementListCaptor.getValue();

        assertThat(results).hasSize(2);

        BigDecimal totalPaidToA = results.stream()
                .filter(s -> s.getPayee().getId().equals(userA.getId()))
                .map(Settlement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(totalPaidToA).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("executeSettlement - Should ignore splits smaller than threshold")
    void executeSettlement_MicroBalances_ShouldBeIgnored() {
        // Arrange
        Expense exp1 = createExpense(userA, "0.001");
        ExpenseSplit split1 = createSplit(exp1, userB, "0.001");

        setupMocksForSettlement(List.of(split1));

        // Act
        settlementService.executeSettlement(requesterId, groupId);

        // Assert
        verify(settlementRepository, never()).saveAll(any());
        verify(expenseSplitRepository).markAllAsSettledByGroupId(groupId);
        verify(expenseRepository).markAllAsSettledByGroupId(groupId);
    }

    @Test
    @DisplayName("executeSettlement - Should handle multiple currencies separately")
    void executeSettlement_MultiCurrency_ShouldSettlePerCurrency() {
        // Arrange
        Expense expUSD = createExpense(userA, "100.00");
        expUSD.setCurrency("USD");
        ExpenseSplit splitUSD = createSplit(expUSD, userB, "100.00");

        Expense expEUR = createExpense(userB, "50.00");
        expEUR.setCurrency("EUR");
        ExpenseSplit splitEUR = createSplit(expEUR, userC, "50.00");

        List<ExpenseSplit> allSplits = List.of(splitUSD, splitEUR);

        setupMocksForSettlement(allSplits);

        // Act
        settlementService.executeSettlement(requesterId, groupId);

        // Assert
        verify(settlementRepository).saveAll(settlementListCaptor.capture());
        List<Settlement> results = settlementListCaptor.getValue();

        assertThat(results).hasSize(2);

        assertThat(results)
                .filteredOn(s -> s.getCurrency().equals("USD"))
                .hasSize(1)
                .first()
                .satisfies(s -> {
                    assertThat(s.getAmount()).isEqualByComparingTo("100.00");
                    assertThat(s.getPayer().getId()).isEqualTo(userB.getId());
                    assertThat(s.getPayee().getId()).isEqualTo(userA.getId());
                });

        assertThat(results)
                .filteredOn(s -> s.getCurrency().equals("EUR"))
                .hasSize(1)
                .first()
                .satisfies(s -> {
                    assertThat(s.getAmount()).isEqualByComparingTo("50.00");
                    assertThat(s.getPayer().getId()).isEqualTo(userC.getId());
                    assertThat(s.getPayee().getId()).isEqualTo(userB.getId());
                });
    }

    @Test
    @DisplayName("executeSettlement - Should throw exception when Group not found")
    void executeSettlement_WhenGroupNotFound_ShouldThrowException() {
        // Arrange
        when(groupRepository.findByIdWithLock(groupId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> settlementService.executeSettlement(requesterId, groupId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Group not found");

        verify(expenseSplitRepository, never()).findUnsettledByGroupId(any());
    }

    @Test
    @DisplayName("executeSettlement - Should throw exception when No Unsettled Expenses")
    void executeSettlement_WhenNoExpenses_ShouldThrowException() {
        // Arrange
        when(groupRepository.findByIdWithLock(groupId)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, requesterId)).thenReturn(true);
        when(expenseSplitRepository.findUnsettledByGroupId(groupId)).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThatThrownBy(() -> settlementService.executeSettlement(requesterId, groupId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No unsettled expenses to settle");

        verify(settlementGroupRepository, never()).save(any());
    }

    @Test
    @DisplayName("executeSettlement - Should throw exception when requester is not a group member")
    void executeSettlement_WhenRequesterNotMember_ShouldThrowException() {
        // Arrange
        when(groupRepository.findByIdWithLock(groupId)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, requesterId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> settlementService.executeSettlement(requesterId, groupId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only group members can settle expenses");

        verify(expenseSplitRepository, never()).findUnsettledByGroupId(any());
    }

    // ==================== getSettlementHistory Tests ====================

    @Test
    @DisplayName("getSettlementHistory - Should return settlements ordered by date")
    void getSettlementHistory_ShouldReturnOrderedList() {
        // Arrange
        SettlementGroup sg1 = SettlementGroup.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .settlements(new ArrayList<>())
                .build();

        SettlementGroup sg2 = SettlementGroup.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .settlements(new ArrayList<>())
                .build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, requesterId)).thenReturn(true);
        when(settlementGroupRepository.findByGroupIdOrderBySettledAtDesc(groupId))
                .thenReturn(List.of(sg1, sg2));

        // Act
        List<SettlementSummaryResponse> result = settlementService.getSettlementHistory(requesterId, groupId);

        // Assert
        assertThat(result).hasSize(2);
        verify(settlementGroupRepository).findByGroupIdOrderBySettledAtDesc(groupId);
    }

    @Test
    @DisplayName("getSettlementHistory - Should throw exception when group not found")
    void getSettlementHistory_WhenGroupNotFound_ShouldThrow() {
        // Arrange
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> settlementService.getSettlementHistory(requesterId, groupId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Group not found");

        verify(settlementGroupRepository, never()).findByGroupIdOrderBySettledAtDesc(any());
    }

    @Test
    @DisplayName("getSettlementHistory - Should return empty list when no settlements exist")
    void getSettlementHistory_WhenNoSettlements_ShouldReturnEmptyList() {
        // Arrange
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, requesterId)).thenReturn(true);
        when(settlementGroupRepository.findByGroupIdOrderBySettledAtDesc(groupId))
                .thenReturn(Collections.emptyList());

        // Act
        List<SettlementSummaryResponse> result = settlementService.getSettlementHistory(requesterId, groupId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getSettlementHistory - Should throw exception when requester is not a group member")
    void getSettlementHistory_WhenRequesterNotMember_ShouldThrow() {
        // Arrange
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, requesterId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> settlementService.getSettlementHistory(requesterId, groupId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only group members can view the settlement history");

        verify(settlementGroupRepository, never()).findByGroupIdOrderBySettledAtDesc(any());
    }

    // ==================== getSettlementDetail Tests ====================

    @Test
    @DisplayName("getSettlementDetail - Should return settlement details")
    void getSettlementDetail_ShouldReturnDetails() {
        // Arrange
        UUID settlementGroupId = UUID.randomUUID();

        Settlement settlement1 = Settlement.builder()
                .id(UUID.randomUUID())
                .payer(userA)
                .payee(userB)
                .amount(new BigDecimal("50.00"))
                .currency("SGD")
                .settlementGroup(testSettlementGroup)
                .build();

        testSettlementGroup.setSettlements(List.of(settlement1));

        when(settlementGroupRepository.findById(settlementGroupId))
                .thenReturn(Optional.of(testSettlementGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, requesterId)).thenReturn(true);

        // Act
        SettlementDetailResponse result = settlementService.getSettlementDetail(requesterId, settlementGroupId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testSettlementGroup.getId());
        assertThat(result.getTransactions()).hasSize(1);
    }

    @Test
    @DisplayName("getSettlementDetail - Should throw when settlement not found")
    void getSettlementDetail_WhenNotFound_ShouldThrow() {
        // Arrange
        UUID settlementGroupId = UUID.randomUUID();
        when(settlementGroupRepository.findById(settlementGroupId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> settlementService.getSettlementDetail(requesterId, settlementGroupId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Settlement not found");
    }

    @Test
    @DisplayName("getSettlementDetail - Should handle empty settlement (perfect wash)")
    void getSettlementDetail_WhenPerfectWash_ShouldReturnEmptyTransactions() {
        // Arrange
        UUID settlementGroupId = UUID.randomUUID();
        testSettlementGroup.setSettlements(Collections.emptyList());

        when(settlementGroupRepository.findById(settlementGroupId))
                .thenReturn(Optional.of(testSettlementGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, requesterId)).thenReturn(true);

        // Act
        SettlementDetailResponse result = settlementService.getSettlementDetail(requesterId, settlementGroupId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTransactions()).isEmpty();
    }

    @Test
    @DisplayName("getSettlementDetail - Should throw exception when requester is not a group member")
    void getSettlementDetail_WhenRequesterNotMember_ShouldThrow() {
        // Arrange
        UUID settlementGroupId = UUID.randomUUID();

        when(settlementGroupRepository.findById(settlementGroupId))
                .thenReturn(Optional.of(testSettlementGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, requesterId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> settlementService.getSettlementDetail(requesterId, settlementGroupId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only group members can view the settlement details");
    }

    // ==================== Helper Methods ====================

    private void setupMocksForSettlement(List<ExpenseSplit> splits) {
        when(groupRepository.findByIdWithLock(groupId)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, requesterId)).thenReturn(true);
        when(expenseSplitRepository.findUnsettledByGroupId(groupId)).thenReturn(splits);

        when(settlementGroupRepository.save(any(SettlementGroup.class)))
                .thenAnswer(invocation -> {
                    SettlementGroup sg = invocation.getArgument(0);
                    if (sg.getId() == null) {
                        sg.setId(UUID.randomUUID());
                    }
                    return sg;
                });

        when(userRepository.findAllById(any())).thenReturn(List.of(userA, userB, userC));

        when(settlementGroupRepository.findById(any(UUID.class)))
                .thenAnswer(invocation -> {
                    SettlementGroup sg = SettlementGroup.builder()
                            .id(invocation.getArgument(0))
                            .group(testGroup)
                            .build();

                    try {
                        List<Settlement> savedSettlements = settlementListCaptor.getValue();
                        sg.setSettlements(savedSettlements);
                    } catch (Exception e) {
                        sg.setSettlements(new ArrayList<>());
                    }

                    return Optional.of(sg);
                });
    }

    private User createUser(String name) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setName(name);
        u.setEmail(name.replace(" ", "").toLowerCase() + "@example.com");
        return u;
    }

    private Expense createExpense(User paidBy, String totalAmount) {
        Expense expense = new Expense() {
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Expense)) return false;
                Expense other = (Expense) o;
                return getId() != null && getId().equals(other.getId());
            }

            @Override
            public int hashCode() {
                return getClass().hashCode();
            }
        };

        expense.setId(UUID.randomUUID());
        expense.setGroup(testGroup);
        expense.setCurrency("SGD");
        expense.setPaidBy(paidBy);
        expense.setTotalAmount(new BigDecimal(totalAmount));

        return expense;
    }

    private ExpenseSplit createSplit(Expense expense, User debtor, String amount) {
        return ExpenseSplit.builder()
                .id(UUID.randomUUID())
                .expense(expense)
                .user(debtor)
                .amountOwed(new BigDecimal(amount))
                .build();
    }
}