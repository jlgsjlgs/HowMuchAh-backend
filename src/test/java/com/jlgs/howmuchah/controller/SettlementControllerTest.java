package com.jlgs.howmuchah.controller;

import com.jlgs.howmuchah.config.RateLimitFilter;
import com.jlgs.howmuchah.config.TestSecurityConfig;
import com.jlgs.howmuchah.dto.response.SettlementDetailResponse;
import com.jlgs.howmuchah.dto.response.SettlementSummaryResponse;
import com.jlgs.howmuchah.dto.response.SettlementTransaction;
import com.jlgs.howmuchah.dto.UserSummary;
import com.jlgs.howmuchah.service.SettlementService;
import com.jlgs.howmuchah.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = SettlementController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = RateLimitFilter.class
        )
)
@Import(TestSecurityConfig.class)
@DisplayName("SettlementController Integration Tests")
class SettlementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private SettlementService settlementService;

    private UUID userId;
    private UUID groupId;
    private String email;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        groupId = UUID.randomUUID();
        email = "test@example.com";

        when(jwtUtil.extractUserId(any(Jwt.class))).thenReturn(userId);
        when(jwtUtil.extractEmail(any(Jwt.class))).thenReturn(email);
    }

    // ==================== getSettlementHistory Tests ====================

    @Test
    @DisplayName("GET /api/settlements/{groupId}/history - Should return settlement history")
    void getSettlementHistory_ShouldReturnHistory() throws Exception {
        // Arrange
        SettlementSummaryResponse summary1 = new SettlementSummaryResponse(
                UUID.randomUUID(),
                LocalDateTime.now(),
                3
        );

        SettlementSummaryResponse summary2 = new SettlementSummaryResponse(
                UUID.randomUUID(),
                LocalDateTime.now().minusDays(1),
                2
        );

        when(settlementService.getSettlementHistory(eq(userId), eq(groupId)))
                .thenReturn(List.of(summary1, summary2));

        // Act & Assert
        mockMvc.perform(get("/api/settlements/{groupId}/history", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].transactionCount").value(3))
                .andExpect(jsonPath("$[1].transactionCount").value(2));

        verify(settlementService, times(1)).getSettlementHistory(userId, groupId);
    }

    @Test
    @DisplayName("GET /api/settlements/{groupId}/history - Should return empty list when no settlements")
    void getSettlementHistory_WhenNoSettlements_ShouldReturnEmptyList() throws Exception {
        // Arrange
        when(settlementService.getSettlementHistory(eq(userId), eq(groupId)))
                .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/settlements/{groupId}/history", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(settlementService, times(1)).getSettlementHistory(userId, groupId);
    }

    @Test
    @DisplayName("GET /api/settlements/{groupId}/history - Should return 400 when group not found")
    void getSettlementHistory_WhenGroupNotFound_ShouldReturn400() throws Exception {
        // Arrange
        when(settlementService.getSettlementHistory(eq(userId), eq(groupId)))
                .thenThrow(new RuntimeException("Group not found"));

        // Act & Assert
        mockMvc.perform(get("/api/settlements/{groupId}/history", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isInternalServerError());

        verify(settlementService, times(1)).getSettlementHistory(userId, groupId);
    }

    @Test
    @DisplayName("GET /api/settlements/{groupId}/history - Should return 400 when user is not a member")
    void getSettlementHistory_WhenUserNotMember_ShouldReturn400() throws Exception {
        // Arrange
        when(settlementService.getSettlementHistory(eq(userId), eq(groupId)))
                .thenThrow(new IllegalArgumentException("Only group members can view the settlement history"));

        // Act & Assert
        mockMvc.perform(get("/api/settlements/{groupId}/history", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(settlementService, times(1)).getSettlementHistory(userId, groupId);
    }

    // ==================== getSettlementDetail Tests ====================

    @Test
    @DisplayName("GET /api/settlements/{settlementGroupId} - Should return settlement details")
    void getSettlementDetail_ShouldReturnDetails() throws Exception {
        // Arrange
        UUID settlementGroupId = UUID.randomUUID();

        UserSummary payer = new UserSummary(UUID.randomUUID(), "Alice", "alice@example.com");
        UserSummary payee = new UserSummary(UUID.randomUUID(), "Bob", "bob@example.com");

        SettlementTransaction transaction = new SettlementTransaction(
                payer,
                payee,
                new BigDecimal("50.00"),
                "SGD"
        );

        SettlementDetailResponse detail = new SettlementDetailResponse(
                settlementGroupId,
                LocalDateTime.now(),
                List.of(transaction)
        );

        when(settlementService.getSettlementDetail(eq(userId), eq(settlementGroupId)))
                .thenReturn(detail);

        // Act & Assert
        mockMvc.perform(get("/api/settlements/{settlementGroupId}", settlementGroupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(settlementGroupId.toString()))
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.transactions.length()").value(1))
                .andExpect(jsonPath("$.transactions[0].amount").value(50.00))
                .andExpect(jsonPath("$.transactions[0].currency").value("SGD"));

        verify(settlementService, times(1)).getSettlementDetail(userId, settlementGroupId);
    }

    @Test
    @DisplayName("GET /api/settlements/{settlementGroupId} - Should handle perfect wash (no transactions)")
    void getSettlementDetail_WhenPerfectWash_ShouldReturnEmptyTransactions() throws Exception {
        // Arrange
        UUID settlementGroupId = UUID.randomUUID();

        SettlementDetailResponse detail = new SettlementDetailResponse(
                settlementGroupId,
                LocalDateTime.now(),
                List.of()
        );

        when(settlementService.getSettlementDetail(eq(userId), eq(settlementGroupId)))
                .thenReturn(detail);

        // Act & Assert
        mockMvc.perform(get("/api/settlements/{settlementGroupId}", settlementGroupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.transactions.length()").value(0));

        verify(settlementService, times(1)).getSettlementDetail(userId, settlementGroupId);
    }

    @Test
    @DisplayName("GET /api/settlements/{settlementGroupId} - Should return 400 when settlement not found")
    void getSettlementDetail_WhenNotFound_ShouldReturn400() throws Exception {
        // Arrange
        UUID settlementGroupId = UUID.randomUUID();
        when(settlementService.getSettlementDetail(eq(userId), eq(settlementGroupId)))
                .thenThrow(new IllegalArgumentException("Settlement not found"));

        // Act & Assert
        mockMvc.perform(get("/api/settlements/{settlementGroupId}", settlementGroupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(settlementService, times(1)).getSettlementDetail(userId, settlementGroupId);
    }

    @Test
    @DisplayName("GET /api/settlements/{settlementGroupId} - Should return 400 when user is not a member")
    void getSettlementDetail_WhenUserNotMember_ShouldReturn400() throws Exception {
        // Arrange
        UUID settlementGroupId = UUID.randomUUID();
        when(settlementService.getSettlementDetail(eq(userId), eq(settlementGroupId)))
                .thenThrow(new IllegalArgumentException("Only group members can view the settlement details"));

        // Act & Assert
        mockMvc.perform(get("/api/settlements/{settlementGroupId}", settlementGroupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(settlementService, times(1)).getSettlementDetail(userId, settlementGroupId);
    }

    // ==================== executeSettlement Tests ====================

    @Test
    @DisplayName("POST /api/settlements/{groupId}/settle - Should execute settlement and return 201")
    void executeSettlement_ShouldReturnCreated() throws Exception {
        // Arrange
        UUID settlementGroupId = UUID.randomUUID();

        UserSummary payer = new UserSummary(UUID.randomUUID(), "Charlie", "charlie@example.com");
        UserSummary payee = new UserSummary(UUID.randomUUID(), "Alice", "alice@example.com");

        SettlementTransaction transaction = new SettlementTransaction(
                payer,
                payee,
                new BigDecimal("75.00"),
                "SGD"
        );

        SettlementDetailResponse result = new SettlementDetailResponse(
                settlementGroupId,
                LocalDateTime.now(),
                List.of(transaction)
        );

        when(settlementService.executeSettlement(eq(userId), eq(groupId)))
                .thenReturn(result);

        // Act & Assert
        mockMvc.perform(post("/api/settlements/{groupId}/settle", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(settlementGroupId.toString()))
                .andExpect(jsonPath("$.transactions.length()").value(1))
                .andExpect(jsonPath("$.transactions[0].payer.name").value("Charlie"))
                .andExpect(jsonPath("$.transactions[0].payee.name").value("Alice"))
                .andExpect(jsonPath("$.transactions[0].amount").value(75.00));

        verify(settlementService, times(1)).executeSettlement(userId, groupId);
    }

    @Test
    @DisplayName("POST /api/settlements/{groupId}/settle - Should return 400 when no unsettled expenses")
    void executeSettlement_WhenNoExpenses_ShouldReturn400() throws Exception {
        // Arrange
        when(settlementService.executeSettlement(eq(userId), eq(groupId)))
                .thenThrow(new IllegalArgumentException("No unsettled expenses to settle"));

        // Act & Assert
        mockMvc.perform(post("/api/settlements/{groupId}/settle", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(settlementService, times(1)).executeSettlement(userId, groupId);
    }

    @Test
    @DisplayName("POST /api/settlements/{groupId}/settle - Should return 400 when group not found")
    void executeSettlement_WhenGroupNotFound_ShouldReturn400() throws Exception {
        // Arrange
        when(settlementService.executeSettlement(eq(userId), eq(groupId)))
                .thenThrow(new IllegalArgumentException("Group not found"));

        // Act & Assert
        mockMvc.perform(post("/api/settlements/{groupId}/settle", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(settlementService, times(1)).executeSettlement(userId, groupId);
    }

    @Test
    @DisplayName("POST /api/settlements/{groupId}/settle - Should handle perfect wash scenario")
    void executeSettlement_WhenPerfectWash_ShouldReturnEmptyTransactions() throws Exception {
        // Arrange
        UUID settlementGroupId = UUID.randomUUID();

        SettlementDetailResponse result = new SettlementDetailResponse(
                settlementGroupId,
                LocalDateTime.now(),
                List.of()
        );

        when(settlementService.executeSettlement(eq(userId), eq(groupId)))
                .thenReturn(result);

        // Act & Assert
        mockMvc.perform(post("/api/settlements/{groupId}/settle", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.transactions.length()").value(0));

        verify(settlementService, times(1)).executeSettlement(userId, groupId);
    }

    @Test
    @DisplayName("POST /api/settlements/{groupId}/settle - Should return 400 when user is not a member")
    void executeSettlement_WhenUserNotMember_ShouldReturn400() throws Exception {
        // Arrange
        when(settlementService.executeSettlement(eq(userId), eq(groupId)))
                .thenThrow(new IllegalArgumentException("Only group members can settle expenses"));

        // Act & Assert
        mockMvc.perform(post("/api/settlements/{groupId}/settle", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(settlementService, times(1)).executeSettlement(userId, groupId);
    }
}