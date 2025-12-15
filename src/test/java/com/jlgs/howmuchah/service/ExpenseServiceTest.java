package com.jlgs.howmuchah.service;

import com.jlgs.howmuchah.dto.request.ExpenseCreationRequest;
import com.jlgs.howmuchah.dto.request.ExpenseSplitDto;
import com.jlgs.howmuchah.dto.request.ExpenseUpdateRequest;
import com.jlgs.howmuchah.dto.response.ExpenseDetailResponse;
import com.jlgs.howmuchah.dto.response.ExpenseResponse;
import com.jlgs.howmuchah.entity.*;
import com.jlgs.howmuchah.repository.ExpenseRepository;
import com.jlgs.howmuchah.repository.ExpenseSplitRepository;
import com.jlgs.howmuchah.repository.GroupMemberRepository;
import com.jlgs.howmuchah.repository.GroupRepository;
import com.jlgs.howmuchah.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService Unit Tests")
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExpenseSplitRepository expenseSplitRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ExpenseService expenseService;

    private UUID groupId;
    private UUID userId1;
    private UUID userId2;
    private UUID userId3;
    private UUID expenseId;
    private User user1;
    private User user2;
    private User user3;
    private Group testGroup;
    private Expense testExpense;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        userId3 = UUID.randomUUID();
        expenseId = UUID.randomUUID();

        user1 = new User();
        user1.setId(userId1);
        user1.setEmail("user1@example.com");
        user1.setName("User One");

        user2 = new User();
        user2.setId(userId2);
        user2.setEmail("user2@example.com");
        user2.setName("User Two");

        user3 = new User();
        user3.setId(userId3);
        user3.setEmail("user3@example.com");
        user3.setName("User Three");

        testGroup = Group.builder()
                .id(groupId)
                .name("Test Group")
                .description("Test Description")
                .owner(user1)
                .build();

        testExpense = Expense.builder()
                .id(expenseId)
                .group(testGroup)
                .description("Test Expense")
                .totalAmount(new BigDecimal("100.00"))
                .currency("SGD")
                .paidBy(user1)
                .category("food")
                .expenseDate(LocalDate.now())
                .isSettled(false)
                .build();
    }

    // ==================== createExpense Tests ====================

    @Test
    @DisplayName("createExpense - Should create expense successfully when valid request")
    void createExpense_WhenValidRequest_ShouldCreateExpense() {
        // Arrange
        List<ExpenseSplitDto> splits = Arrays.asList(
                new ExpenseSplitDto(userId1, new BigDecimal("50.00")),
                new ExpenseSplitDto(userId2, new BigDecimal("50.00"))
        );

        ExpenseCreationRequest request = new ExpenseCreationRequest(
                groupId,
                "Dinner",
                new BigDecimal("100.00"),
                "SGD",
                userId1,
                "food",
                LocalDate.now(),
                splits
        );

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId1)).thenReturn(true);
        when(groupMemberRepository.findByGroupId(groupId)).thenReturn(Arrays.asList(
                createGroupMember(user1),
                createGroupMember(user2)
        ));
        when(userRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);
        when(expenseSplitRepository.saveAll(any())).thenReturn(Arrays.asList(
                createExpenseSplit(user1, new BigDecimal("50.00")),
                createExpenseSplit(user2, new BigDecimal("50.00"))
        ));

        // Act
        ExpenseDetailResponse result = expenseService.createExpense(userId1, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEqualTo("Test Expense");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("100.00");

        verify(groupRepository, times(1)).findById(groupId);
        verify(groupMemberRepository, times(2)).existsByGroupIdAndUserId(groupId, userId1);
        verify(expenseRepository, times(1)).save(any(Expense.class));
        verify(expenseSplitRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("createExpense - Should throw exception when user not part of group")
    void createExpense_WhenUserNotPartOfGroup_ShouldThrowException() {
        // Arrange
        UUID maliciousUserId = UUID.randomUUID();
        List<ExpenseSplitDto> splits = List.of(
                new ExpenseSplitDto(userId1, new BigDecimal("100.00"))
        );

        ExpenseCreationRequest request = new ExpenseCreationRequest(
                groupId,
                "Dinner",
                new BigDecimal("100.00"),
                "SGD",
                userId1,
                "food",
                LocalDate.now(),
                splits
        );

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, maliciousUserId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> expenseService.createExpense(maliciousUserId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only group members can add expenses for the group");

        verify(groupRepository, times(1)).findById(groupId);
        verify(groupMemberRepository, times(1)).existsByGroupIdAndUserId(groupId, maliciousUserId);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    @DisplayName("createExpense - Should throw exception when payer not part of group")
    void createExpense_WhenPayerNotPartOfGroup_ShouldThrowException() {
        // Arrange
        UUID nonMemberPayerId = UUID.randomUUID();
        List<ExpenseSplitDto> splits = List.of(
                new ExpenseSplitDto(userId1, new BigDecimal("100.00"))
        );

        ExpenseCreationRequest request = new ExpenseCreationRequest(
                groupId,
                "Dinner",
                new BigDecimal("100.00"),
                "SGD",
                nonMemberPayerId,
                "food",
                LocalDate.now(),
                splits
        );

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId1)).thenReturn(true);
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, nonMemberPayerId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> expenseService.createExpense(userId1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payer must be a current group member");

        verify(expenseRepository, never()).save(any());
    }

    @Test
    @DisplayName("createExpense - Should throw exception when participant not part of group")
    void createExpense_WhenParticipantNotPartOfGroup_ShouldThrowException() {
        // Arrange
        UUID nonMemberParticipantId = UUID.randomUUID();
        List<ExpenseSplitDto> splits = Arrays.asList(
                new ExpenseSplitDto(userId1, new BigDecimal("50.00")),
                new ExpenseSplitDto(nonMemberParticipantId, new BigDecimal("50.00"))
        );

        ExpenseCreationRequest request = new ExpenseCreationRequest(
                groupId,
                "Dinner",
                new BigDecimal("100.00"),
                "SGD",
                userId1,
                "food",
                LocalDate.now(),
                splits
        );

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId1)).thenReturn(true);
        when(groupMemberRepository.findByGroupId(groupId)).thenReturn(List.of(
                createGroupMember(user1)
        ));

        // Act & Assert
        assertThatThrownBy(() -> expenseService.createExpense(userId1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("All expense participants must be current group members");

        verify(expenseRepository, never()).save(any());
    }

    @Test
    @DisplayName("createExpense - Should throw exception when splits don't sum to total")
    void createExpense_WhenSplitsDontSumToTotal_ShouldThrowException() {
        // Arrange
        List<ExpenseSplitDto> splits = Arrays.asList(
                new ExpenseSplitDto(userId1, new BigDecimal("40.00")),
                new ExpenseSplitDto(userId2, new BigDecimal("50.00"))
        );

        ExpenseCreationRequest request = new ExpenseCreationRequest(
                groupId,
                "Dinner",
                new BigDecimal("100.00"),
                "SGD",
                userId1,
                "food",
                LocalDate.now(),
                splits
        );

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId1)).thenReturn(true);
        when(groupMemberRepository.findByGroupId(groupId)).thenReturn(Arrays.asList(
                createGroupMember(user1),
                createGroupMember(user2)
        ));

        // Act & Assert
        assertThatThrownBy(() -> expenseService.createExpense(userId1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Split amounts")
                .hasMessageContaining("must equal total amount");

        verify(expenseRepository, never()).save(any());
    }

    @Test
    @DisplayName("createExpense - Should throw exception when splits list is empty")
    void createExpense_WhenSplitsEmpty_ShouldThrowException() {
        // Arrange
        ExpenseCreationRequest request = new ExpenseCreationRequest(
                groupId,
                "Dinner",
                new BigDecimal("100.00"),
                "SGD",
                userId1,
                "food",
                LocalDate.now(),
                List.of()
        );

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId1)).thenReturn(true);
        when(groupMemberRepository.findByGroupId(groupId)).thenReturn(List.of(
                createGroupMember(user1)
        ));

        // Act & Assert
        assertThatThrownBy(() -> expenseService.createExpense(userId1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expense must have at least one split");

        verify(expenseRepository, never()).save(any());
    }

    @Test
    @DisplayName("createExpense - Should throw exception when group not found")
    void createExpense_WhenGroupNotFound_ShouldThrowException() {
        // Arrange
        ExpenseCreationRequest request = new ExpenseCreationRequest(
                groupId,
                "Dinner",
                new BigDecimal("100.00"),
                "SGD",
                userId1,
                "food",
                LocalDate.now(),
                List.of(new ExpenseSplitDto(userId1, new BigDecimal("100.00")))
        );

        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> expenseService.createExpense(userId1, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Group not found");

        verify(expenseRepository, never()).save(any());
    }

    // ==================== updateExpense Tests ====================

    @Test
    @DisplayName("updateExpense - Should update expense successfully when valid request")
    void updateExpense_WhenValidRequest_ShouldUpdateExpense() {
        // Arrange
        List<ExpenseSplitDto> splits = Arrays.asList(
                new ExpenseSplitDto(userId1, new BigDecimal("60.00")),
                new ExpenseSplitDto(userId2, new BigDecimal("60.00"))
        );

        ExpenseUpdateRequest request = new ExpenseUpdateRequest(
                "Updated Dinner",
                new BigDecimal("120.00"),
                "SGD",
                userId1,
                "food",
                LocalDate.now(),
                splits
        );

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(testExpense));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId1)).thenReturn(true);
        when(groupMemberRepository.findByGroupId(groupId)).thenReturn(Arrays.asList(
                createGroupMember(user1),
                createGroupMember(user2)
        ));
        when(userRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);
        when(expenseSplitRepository.saveAll(any())).thenReturn(Arrays.asList(
                createExpenseSplit(user1, new BigDecimal("60.00")),
                createExpenseSplit(user2, new BigDecimal("60.00"))
        ));

        // Act
        ExpenseDetailResponse result = expenseService.updateExpense(userId1, expenseId, request);

        // Assert
        assertThat(result).isNotNull();
        verify(expenseRepository, times(1)).findById(expenseId);
        verify(expenseRepository, times(1)).save(any(Expense.class));
        verify(expenseSplitRepository, times(1)).deleteByExpenseId(expenseId);
        verify(expenseSplitRepository, times(1)).flush();
        verify(expenseSplitRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("updateExpense - Should throw exception when user not part of group")
    void updateExpense_WhenUserNotPartOfGroup_ShouldThrowException() {
        // Arrange
        UUID maliciousUserId = UUID.randomUUID();
        ExpenseUpdateRequest request = new ExpenseUpdateRequest(
                "Updated Dinner",
                new BigDecimal("120.00"),
                "SGD",
                userId1,
                "food",
                LocalDate.now(),
                List.of(new ExpenseSplitDto(userId1, new BigDecimal("120.00")))
        );

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(testExpense));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, maliciousUserId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> expenseService.updateExpense(maliciousUserId, expenseId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only group members can update expenses for the group");

        verify(expenseRepository, times(1)).findById(expenseId);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateExpense - Should throw exception when expense is settled")
    void updateExpense_WhenExpenseSettled_ShouldThrowException() {
        // Arrange
        testExpense.setSettled(true);

        ExpenseUpdateRequest request = new ExpenseUpdateRequest(
                "Updated Dinner",
                new BigDecimal("120.00"),
                "SGD",
                userId1,
                "food",
                LocalDate.now(),
                List.of(new ExpenseSplitDto(userId1, new BigDecimal("120.00")))
        );

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(testExpense));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId1)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> expenseService.updateExpense(userId1, expenseId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot modify settled expense.");

        verify(expenseRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateExpense - Should throw exception when new payer not part of group")
    void updateExpense_WhenNewPayerNotPartOfGroup_ShouldThrowException() {
        // Arrange
        UUID nonMemberPayerId = UUID.randomUUID();
        ExpenseUpdateRequest request = new ExpenseUpdateRequest(
                "Updated Dinner",
                new BigDecimal("120.00"),
                "SGD",
                nonMemberPayerId,
                "food",
                LocalDate.now(),
                List.of(new ExpenseSplitDto(userId1, new BigDecimal("120.00")))
        );

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(testExpense));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId1)).thenReturn(true);
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, nonMemberPayerId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> expenseService.updateExpense(userId1, expenseId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payer must be a current group member");

        verify(expenseRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateExpense - Should throw exception when new participant not part of group")
    void updateExpense_WhenNewParticipantNotPartOfGroup_ShouldThrowException() {
        // Arrange
        UUID nonMemberParticipantId = UUID.randomUUID();
        List<ExpenseSplitDto> splits = Arrays.asList(
                new ExpenseSplitDto(userId1, new BigDecimal("60.00")),
                new ExpenseSplitDto(nonMemberParticipantId, new BigDecimal("60.00"))
        );

        ExpenseUpdateRequest request = new ExpenseUpdateRequest(
                "Updated Dinner",
                new BigDecimal("120.00"),
                "SGD",
                userId1,
                "food",
                LocalDate.now(),
                splits
        );

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(testExpense));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId1)).thenReturn(true);
        when(groupMemberRepository.findByGroupId(groupId)).thenReturn(List.of(
                createGroupMember(user1)
        ));

        // Act & Assert
        assertThatThrownBy(() -> expenseService.updateExpense(userId1, expenseId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("All expense participants must be current group members");

        verify(expenseRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateExpense - Should throw exception when expense not found")
    void updateExpense_WhenExpenseNotFound_ShouldThrowException() {
        // Arrange
        ExpenseUpdateRequest request = new ExpenseUpdateRequest(
                "Updated Dinner",
                new BigDecimal("120.00"),
                "SGD",
                userId1,
                "food",
                LocalDate.now(),
                List.of(new ExpenseSplitDto(userId1, new BigDecimal("120.00")))
        );

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> expenseService.updateExpense(userId1, expenseId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Expense not found");

        verify(expenseRepository, never()).save(any());
    }

    // ==================== getExpensesByGroup Tests ====================

    @Test
    @DisplayName("getExpensesByGroup - Should return expenses when user is group member")
    void getExpensesByGroup_WhenUserIsGroupMember_ShouldReturnExpenses() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Page<Expense> expensePage = new PageImpl<>(List.of(testExpense));

        when(groupRepository.existsById(groupId)).thenReturn(true);
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId1)).thenReturn(true);
        when(expenseRepository.findByGroupId(groupId, pageable)).thenReturn(expensePage);

        // Act
        Page<ExpenseResponse> result = expenseService.getExpensesByGroup(userId1, groupId, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(groupRepository, times(1)).existsById(groupId);
        verify(groupMemberRepository, times(1)).existsByGroupIdAndUserId(groupId, userId1);
        verify(expenseRepository, times(1)).findByGroupId(groupId, pageable);
    }

    @Test
    @DisplayName("getExpensesByGroup - Should throw exception when user not part of group")
    void getExpensesByGroup_WhenUserNotPartOfGroup_ShouldThrowException() {
        // Arrange
        UUID maliciousUserId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        when(groupRepository.existsById(groupId)).thenReturn(true);
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, maliciousUserId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> expenseService.getExpensesByGroup(maliciousUserId, groupId, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only group members can access expenses for the group");

        verify(groupRepository, times(1)).existsById(groupId);
        verify(groupMemberRepository, times(1)).existsByGroupIdAndUserId(groupId, maliciousUserId);
        verify(expenseRepository, never()).findByGroupId(any(), any());
    }

    @Test
    @DisplayName("getExpensesByGroup - Should throw exception when group not found")
    void getExpensesByGroup_WhenGroupNotFound_ShouldThrowException() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        when(groupRepository.existsById(groupId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> expenseService.getExpensesByGroup(userId1, groupId, pageable))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Group not found");

        verify(groupRepository, times(1)).existsById(groupId);
        verify(expenseRepository, never()).findByGroupId(any(), any());
    }

    // ==================== getExpenseDetail Tests ====================

    @Test
    @DisplayName("getExpenseDetail - Should return expense detail when user is group member")
    void getExpenseDetail_WhenUserIsGroupMember_ShouldReturnExpenseDetail() {
        // Arrange
        List<ExpenseSplit> splits = List.of(
                createExpenseSplit(user1, new BigDecimal("50.00")),
                createExpenseSplit(user2, new BigDecimal("50.00"))
        );

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(testExpense));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId1)).thenReturn(true);
        when(expenseSplitRepository.findByExpenseId(expenseId)).thenReturn(splits);

        // Act
        ExpenseDetailResponse result = expenseService.getExpenseDetail(userId1, expenseId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSplits()).hasSize(2);

        verify(expenseRepository, times(1)).findById(expenseId);
        verify(groupMemberRepository, times(1)).existsByGroupIdAndUserId(groupId, userId1);
        verify(expenseSplitRepository, times(1)).findByExpenseId(expenseId);
    }

    @Test
    @DisplayName("getExpenseDetail - Should throw exception when user not part of group")
    void getExpenseDetail_WhenUserNotPartOfGroup_ShouldThrowException() {
        // Arrange
        UUID maliciousUserId = UUID.randomUUID();

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(testExpense));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, maliciousUserId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> expenseService.getExpenseDetail(maliciousUserId, expenseId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only group members can access information for this expense");

        verify(expenseRepository, times(1)).findById(expenseId);
        verify(groupMemberRepository, times(1)).existsByGroupIdAndUserId(groupId, maliciousUserId);
        verify(expenseSplitRepository, never()).findByExpenseId(any());
    }

    @Test
    @DisplayName("getExpenseDetail - Should throw exception when expense not found")
    void getExpenseDetail_WhenExpenseNotFound_ShouldThrowException() {
        // Arrange
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> expenseService.getExpenseDetail(userId1, expenseId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Expense not found");

        verify(expenseRepository, times(1)).findById(expenseId);
        verify(expenseSplitRepository, never()).findByExpenseId(any());
    }

    // ==================== deleteExpense Tests ====================

    @Test
    @DisplayName("deleteExpense - Should delete expense when user is group member")
    void deleteExpense_WhenUserIsGroupMember_ShouldDeleteExpense() {
        // Arrange
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(testExpense));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId1)).thenReturn(true);

        // Act
        expenseService.deleteExpense(userId1, expenseId);

        // Assert
        verify(expenseRepository, times(1)).findById(expenseId);
        verify(groupMemberRepository, times(1)).existsByGroupIdAndUserId(groupId, userId1);
        verify(expenseRepository, times(1)).delete(testExpense);
    }

    @Test
    @DisplayName("deleteExpense - Should throw exception when user not part of group")
    void deleteExpense_WhenUserNotPartOfGroup_ShouldThrowException() {
        // Arrange
        UUID maliciousUserId = UUID.randomUUID();

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(testExpense));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, maliciousUserId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> expenseService.deleteExpense(maliciousUserId, expenseId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only group members can delete this expense");

        verify(expenseRepository, times(1)).findById(expenseId);
        verify(groupMemberRepository, times(1)).existsByGroupIdAndUserId(groupId, maliciousUserId);
        verify(expenseRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteExpense - Should throw exception when expense is settled")
    void deleteExpense_WhenExpenseSettled_ShouldThrowException() {
        // Arrange
        testExpense.setSettled(true);

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(testExpense));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId1)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> expenseService.deleteExpense(userId1, expenseId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot delete settled expense.");

        verify(expenseRepository, times(1)).findById(expenseId);
        verify(expenseRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteExpense - Should throw exception when expense not found")
    void deleteExpense_WhenExpenseNotFound_ShouldThrowException() {
        // Arrange
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> expenseService.deleteExpense(userId1, expenseId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Expense not found");

        verify(expenseRepository, times(1)).findById(expenseId);
        verify(expenseRepository, never()).delete(any());
    }

    // ==================== Helper Methods ====================

    private GroupMember createGroupMember(User user) {
        GroupMember gm = new GroupMember();
        gm.setUser(user);
        gm.setGroup(testGroup);
        return gm;
    }

    private ExpenseSplit createExpenseSplit(User user, BigDecimal amount) {
        return ExpenseSplit.builder()
                .id(UUID.randomUUID())
                .expense(testExpense)
                .user(user)
                .amountOwed(amount)
                .isSettled(false)
                .build();
    }
}