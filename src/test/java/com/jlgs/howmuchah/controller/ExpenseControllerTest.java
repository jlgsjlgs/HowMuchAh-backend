package com.jlgs.howmuchah.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlgs.howmuchah.config.RateLimitFilter;
import com.jlgs.howmuchah.config.TestSecurityConfig;
import com.jlgs.howmuchah.dto.UserSummary;
import com.jlgs.howmuchah.dto.request.ExpenseCreationRequest;
import com.jlgs.howmuchah.dto.request.ExpenseSplitDto;
import com.jlgs.howmuchah.dto.request.ExpenseUpdateRequest;
import com.jlgs.howmuchah.dto.response.ExpenseDetailResponse;
import com.jlgs.howmuchah.dto.response.ExpenseResponse;
import com.jlgs.howmuchah.dto.response.ExpenseSplitResponse;
import com.jlgs.howmuchah.service.ExpenseService;
import com.jlgs.howmuchah.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ExpenseController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = RateLimitFilter.class
        )
)
@Import(TestSecurityConfig.class)
@DisplayName("ExpenseController Integration Tests")
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private ExpenseService expenseService;

    private UUID userId;
    private UUID groupId;
    private UUID expenseId;
    private String email;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        groupId = UUID.randomUUID();
        expenseId = UUID.randomUUID();
        email = "test@example.com";

        // Mock JwtUtil to return test values
        when(jwtUtil.extractUserId(any(Jwt.class))).thenReturn(userId);
        when(jwtUtil.extractEmail(any(Jwt.class))).thenReturn(email);
    }

    // ==================== createExpense Tests ====================

    @Test
    @DisplayName("POST /api/expenses - Should create expense and return 201")
    void createExpense_WhenValidRequest_ShouldReturn201() throws Exception {
        // Arrange
        List<ExpenseSplitDto> splits = Arrays.asList(
                new ExpenseSplitDto(userId, new BigDecimal("50.00")),
                new ExpenseSplitDto(UUID.randomUUID(), new BigDecimal("50.00"))
        );

        ExpenseCreationRequest request = new ExpenseCreationRequest(
                groupId,
                "Dinner at Restaurant",
                new BigDecimal("100.00"),
                "SGD",
                userId,
                "food",
                LocalDate.now(),
                splits
        );

        UserSummary paidBy = new UserSummary(userId, "Test User", email);
        List<ExpenseSplitResponse> splitResponses = Arrays.asList(
                new ExpenseSplitResponse(UUID.randomUUID(), paidBy, new BigDecimal("50.00"), false),
                new ExpenseSplitResponse(UUID.randomUUID(),
                        new UserSummary(UUID.randomUUID(), "User Two", "user2@example.com"),
                        new BigDecimal("50.00"), false)
        );

        ExpenseDetailResponse response = new ExpenseDetailResponse(
                expenseId,
                groupId,
                "Dinner at Restaurant",
                new BigDecimal("100.00"),
                "SGD",
                "food",
                LocalDate.now(),
                paidBy,
                splitResponses,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(expenseService.createExpense(eq(userId), any(ExpenseCreationRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(expenseId.toString()))
                .andExpect(jsonPath("$.description").value("Dinner at Restaurant"))
                .andExpect(jsonPath("$.totalAmount").value(100.00))
                .andExpect(jsonPath("$.currency").value("SGD"))
                .andExpect(jsonPath("$.category").value("food"))
                .andExpect(jsonPath("$.splits.length()").value(2));

        verify(expenseService, times(1)).createExpense(eq(userId), any(ExpenseCreationRequest.class));
    }

    @Test
    @DisplayName("POST /api/expenses - Should return 400 when user not part of group")
    void createExpense_WhenUserNotPartOfGroup_ShouldReturn400() throws Exception {
        // Arrange
        ExpenseCreationRequest request = new ExpenseCreationRequest(
                groupId,
                "Dinner",
                new BigDecimal("100.00"),
                "SGD",
                userId,
                "food",
                LocalDate.now(),
                List.of(new ExpenseSplitDto(userId, new BigDecimal("100.00")))
        );

        when(expenseService.createExpense(eq(userId), any(ExpenseCreationRequest.class)))
                .thenThrow(new IllegalArgumentException("Only group members can add expenses for the group"));

        // Act & Assert
        mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(expenseService, times(1)).createExpense(eq(userId), any(ExpenseCreationRequest.class));
    }

    @Test
    @DisplayName("POST /api/expenses - Should return 400 when splits don't sum to total")
    void createExpense_WhenSplitsDontSumToTotal_ShouldReturn400() throws Exception {
        // Arrange
        ExpenseCreationRequest request = new ExpenseCreationRequest(
                groupId,
                "Dinner",
                new BigDecimal("100.00"),
                "SGD",
                userId,
                "food",
                LocalDate.now(),
                List.of(new ExpenseSplitDto(userId, new BigDecimal("50.00")))
        );

        when(expenseService.createExpense(eq(userId), any(ExpenseCreationRequest.class)))
                .thenThrow(new IllegalArgumentException("Split amounts (50.00) must equal total amount (100.00)"));

        // Act & Assert
        mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(expenseService, times(1)).createExpense(eq(userId), any(ExpenseCreationRequest.class));
    }

    @Test
    @DisplayName("POST /api/expenses - Should return 400 when participant not part of group")
    void createExpense_WhenParticipantNotPartOfGroup_ShouldReturn400() throws Exception {
        // Arrange
        ExpenseCreationRequest request = new ExpenseCreationRequest(
                groupId,
                "Dinner",
                new BigDecimal("100.00"),
                "SGD",
                userId,
                "food",
                LocalDate.now(),
                List.of(
                        new ExpenseSplitDto(userId, new BigDecimal("50.00")),
                        new ExpenseSplitDto(UUID.randomUUID(), new BigDecimal("50.00"))
                )
        );

        when(expenseService.createExpense(eq(userId), any(ExpenseCreationRequest.class)))
                .thenThrow(new IllegalArgumentException("All expense participants must be current group members"));

        // Act & Assert
        mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(expenseService, times(1)).createExpense(eq(userId), any(ExpenseCreationRequest.class));
    }

    // ==================== getExpensesByGroup Tests ====================

    @Test
    @DisplayName("GET /api/expenses - Should return paginated expenses for group")
    void getExpensesByGroup_WhenUserIsGroupMember_ShouldReturn200() throws Exception {
        // Arrange
        ExpenseResponse expense1 = new ExpenseResponse(
                UUID.randomUUID(),
                "Dinner",
                new BigDecimal("100.00"),
                "SGD",
                "food",
                LocalDate.now(),
                userId,
                "Test User",
                LocalDateTime.now(),
                LocalDateTime.now(),
                false
        );

        ExpenseResponse expense2 = new ExpenseResponse(
                UUID.randomUUID(),
                "Lunch",
                new BigDecimal("50.00"),
                "SGD",
                "food",
                LocalDate.now(),
                userId,
                "Test User",
                LocalDateTime.now(),
                LocalDateTime.now(),
                false
        );

        Page<ExpenseResponse> expensePage = new PageImpl<>(
                Arrays.asList(expense1, expense2),
                PageRequest.of(0, 20),
                2
        );

        when(expenseService.getExpensesByGroup(eq(userId), eq(groupId), any()))
                .thenReturn(expensePage);

        // Act & Assert
        mockMvc.perform(get("/api/expenses")
                        .param("groupId", groupId.toString())
                        .param("page", "0")
                        .param("size", "20")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].description").value("Dinner"))
                .andExpect(jsonPath("$.content[1].description").value("Lunch"));

        verify(expenseService, times(1)).getExpensesByGroup(eq(userId), eq(groupId), any());
    }

    @Test
    @DisplayName("GET /api/expenses - Should return 400 when user not part of group")
    void getExpensesByGroup_WhenUserNotPartOfGroup_ShouldReturn400() throws Exception {
        // Arrange
        when(expenseService.getExpensesByGroup(eq(userId), eq(groupId), any()))
                .thenThrow(new IllegalArgumentException("Only group members can access expenses for the group"));

        // Act & Assert
        mockMvc.perform(get("/api/expenses")
                        .param("groupId", groupId.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(expenseService, times(1)).getExpensesByGroup(eq(userId), eq(groupId), any());
    }

    @Test
    @DisplayName("GET /api/expenses - Should return 500 when group not found")
    void getExpensesByGroup_WhenGroupNotFound_ShouldReturn500() throws Exception {
        // Arrange
        when(expenseService.getExpensesByGroup(eq(userId), eq(groupId), any()))
                .thenThrow(new RuntimeException("Group not found"));

        // Act & Assert
        mockMvc.perform(get("/api/expenses")
                        .param("groupId", groupId.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isInternalServerError());

        verify(expenseService, times(1)).getExpensesByGroup(eq(userId), eq(groupId), any());
    }

    // ==================== getExpenseDetail Tests ====================

    @Test
    @DisplayName("GET /api/expenses/{expenseId} - Should return expense detail")
    void getExpenseDetail_WhenUserIsGroupMember_ShouldReturn200() throws Exception {
        // Arrange
        UserSummary paidBy = new UserSummary(userId, "Test User", email);
        ExpenseDetailResponse response = new ExpenseDetailResponse(
                expenseId,
                groupId,
                "Dinner at Restaurant",
                new BigDecimal("100.00"),
                "SGD",
                "food",
                LocalDate.now(),
                paidBy,
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(expenseService.getExpenseDetail(userId, expenseId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/expenses/{expenseId}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expenseId.toString()))
                .andExpect(jsonPath("$.description").value("Dinner at Restaurant"))
                .andExpect(jsonPath("$.totalAmount").value(100.00));

        verify(expenseService, times(1)).getExpenseDetail(userId, expenseId);
    }

    @Test
    @DisplayName("GET /api/expenses/{expenseId} - Should return 400 when user not part of group")
    void getExpenseDetail_WhenUserNotPartOfGroup_ShouldReturn400() throws Exception {
        // Arrange
        when(expenseService.getExpenseDetail(userId, expenseId))
                .thenThrow(new IllegalArgumentException("Only group members can access information for this expense"));

        // Act & Assert
        mockMvc.perform(get("/api/expenses/{expenseId}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(expenseService, times(1)).getExpenseDetail(userId, expenseId);
    }

    @Test
    @DisplayName("GET /api/expenses/{expenseId} - Should return 500 when expense not found")
    void getExpenseDetail_WhenExpenseNotFound_ShouldReturn500() throws Exception {
        // Arrange
        when(expenseService.getExpenseDetail(userId, expenseId))
                .thenThrow(new RuntimeException("Expense not found"));

        // Act & Assert
        mockMvc.perform(get("/api/expenses/{expenseId}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isInternalServerError());

        verify(expenseService, times(1)).getExpenseDetail(userId, expenseId);
    }

    // ==================== getUnsettledExpensesCount Tests ====================

    @Test
    @DisplayName("GET /api/expenses/{groupId}/unsettled - Should return count when user is group member")
    void getUnsettledExpensesCount_WhenUserIsGroupMember_ShouldReturn200() throws Exception {
        // Arrange
        when(expenseService.getUnsettledExpensesCount(userId, groupId)).thenReturn(5L);

        // Act & Assert
        mockMvc.perform(get("/api/expenses/{groupId}/unsettled", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));

        verify(expenseService, times(1)).getUnsettledExpensesCount(userId, groupId);
    }

    @Test
    @DisplayName("GET /api/expenses/{groupId}/unsettled - Should return zero when no unsettled expenses")
    void getUnsettledExpensesCount_WhenNoUnsettledExpenses_ShouldReturnZero() throws Exception {
        // Arrange
        when(expenseService.getUnsettledExpensesCount(userId, groupId)).thenReturn(0L);

        // Act & Assert
        mockMvc.perform(get("/api/expenses/{groupId}/unsettled", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));

        verify(expenseService, times(1)).getUnsettledExpensesCount(userId, groupId);
    }

    @Test
    @DisplayName("GET /api/expenses/{groupId}/unsettled - Should return 400 when user not group member")
    void getUnsettledExpensesCount_WhenUserNotGroupMember_ShouldReturn400() throws Exception {
        // Arrange
        when(expenseService.getUnsettledExpensesCount(userId, groupId))
                .thenThrow(new IllegalArgumentException("User is not a member of this group"));

        // Act & Assert
        mockMvc.perform(get("/api/expenses/{groupId}/unsettled", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(expenseService, times(1)).getUnsettledExpensesCount(userId, groupId);
    }

    // ==================== deleteExpense Tests ====================

    @Test
    @DisplayName("DELETE /api/expenses/{expenseId} - Should delete expense and return 204")
    void deleteExpense_WhenUserIsGroupMember_ShouldReturn204() throws Exception {
        // Arrange
        doNothing().when(expenseService).deleteExpense(userId, expenseId);

        // Act & Assert
        mockMvc.perform(delete("/api/expenses/{expenseId}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isNoContent());

        verify(expenseService, times(1)).deleteExpense(userId, expenseId);
    }

    @Test
    @DisplayName("DELETE /api/expenses/{expenseId} - Should return 400 when user not part of group")
    void deleteExpense_WhenUserNotPartOfGroup_ShouldReturn400() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("Only group members can delete this expense"))
                .when(expenseService).deleteExpense(userId, expenseId);

        // Act & Assert
        mockMvc.perform(delete("/api/expenses/{expenseId}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(expenseService, times(1)).deleteExpense(userId, expenseId);
    }

    @Test
    @DisplayName("DELETE /api/expenses/{expenseId} - Should return 400 when expense is settled")
    void deleteExpense_WhenExpenseSettled_ShouldReturn400() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("Cannot delete settled expense."))
                .when(expenseService).deleteExpense(userId, expenseId);

        // Act & Assert
        mockMvc.perform(delete("/api/expenses/{expenseId}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(expenseService, times(1)).deleteExpense(userId, expenseId);
    }

    @Test
    @DisplayName("DELETE /api/expenses/{expenseId} - Should return 500 when expense not found")
    void deleteExpense_WhenExpenseNotFound_ShouldReturn500() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Expense not found"))
                .when(expenseService).deleteExpense(userId, expenseId);

        // Act & Assert
        mockMvc.perform(delete("/api/expenses/{expenseId}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isInternalServerError());

        verify(expenseService, times(1)).deleteExpense(userId, expenseId);
    }
}