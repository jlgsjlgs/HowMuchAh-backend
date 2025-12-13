package com.jlgs.howmuchah.controller;

import com.jlgs.howmuchah.config.TestSecurityConfig;
import com.jlgs.howmuchah.entity.Group;
import com.jlgs.howmuchah.entity.Invitation;
import com.jlgs.howmuchah.entity.User;
import com.jlgs.howmuchah.enums.InvitationStatus;
import com.jlgs.howmuchah.service.InvitationService;
import com.jlgs.howmuchah.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InvitationController.class)
@Import(TestSecurityConfig.class)
@DisplayName("InvitationController Integration Tests")
class InvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private InvitationService invitationService;

    private UUID userId;
    private UUID invitationId;
    private String email;
    private User invitedUser;
    private User owner;
    private Group testGroup;
    private Invitation testInvitation;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        invitationId = UUID.randomUUID();
        email = "invited@example.com";

        invitedUser = new User();
        invitedUser.setId(userId);
        invitedUser.setEmail(email);
        invitedUser.setName("Invited User");

        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("owner@example.com");
        owner.setName("Group Owner");

        testGroup = Group.builder()
                .id(UUID.randomUUID())
                .name("Test Group")
                .description("Test Description")
                .owner(owner)
                .build();

        testInvitation = Invitation.builder()
                .id(invitationId)
                .group(testGroup)
                .invitedEmail(email)
                .invitedBy(owner)
                .status(InvitationStatus.PENDING)
                .build();

        // Mock JwtUtil to return test values
        when(jwtUtil.extractUserId(any(Jwt.class))).thenReturn(userId);
        when(jwtUtil.extractEmail(any(Jwt.class))).thenReturn(email);
    }

    // ==================== getPendingInvitations Tests ====================

    @Test
    @DisplayName("GET /api/invitations/pending - Should return pending invitations for user")
    void getPendingInvitations_WhenInvitationsExist_ShouldReturn200() throws Exception {
        // Arrange
        Invitation inv1 = Invitation.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .invitedEmail(email)
                .invitedBy(owner)
                .status(InvitationStatus.PENDING)
                .build();
        Invitation inv2 = Invitation.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .invitedEmail(email)
                .invitedBy(owner)
                .status(InvitationStatus.PENDING)
                .build();
        List<Invitation> invitations = Arrays.asList(inv1, inv2);

        when(invitationService.getPendingInvitationsForEmail(email)).thenReturn(invitations);

        // Act & Assert
        mockMvc.perform(get("/api/invitations/pending")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].invitedEmail").value(email))
                .andExpect(jsonPath("$[1].invitedEmail").value(email))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].status").value("PENDING"));

        verify(invitationService, times(1)).getPendingInvitationsForEmail(email);
    }

    @Test
    @DisplayName("GET /api/invitations/pending - Should return empty list when no pending invitations")
    void getPendingInvitations_WhenNoInvitations_ShouldReturnEmptyList() throws Exception {
        // Arrange
        when(invitationService.getPendingInvitationsForEmail(email)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/invitations/pending")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(invitationService, times(1)).getPendingInvitationsForEmail(email);
    }

    // ==================== acceptInvitation Tests ====================

    @Test
    @DisplayName("POST /api/invitations/{invitationId}/accept - Should accept invitation and return 200")
    void acceptInvitation_WhenValidRequest_ShouldReturn200() throws Exception {
        // Arrange
        testInvitation.setStatus(InvitationStatus.ACCEPTED);
        when(invitationService.acceptInvitation(invitationId, userId, email))
                .thenReturn(testInvitation);

        // Act & Assert
        mockMvc.perform(post("/api/invitations/{invitationId}/accept", invitationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invitationId.toString()))
                .andExpect(jsonPath("$.invitedEmail").value(email))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        verify(invitationService, times(1)).acceptInvitation(invitationId, userId, email);
    }

    @Test
    @DisplayName("POST /api/invitations/{invitationId}/accept - Should return 400 when invitation not found")
    void acceptInvitation_WhenInvitationNotFound_ShouldReturn400() throws Exception {
        // Arrange
        when(invitationService.acceptInvitation(invitationId, userId, email))
                .thenThrow(new IllegalArgumentException("Invitation not found"));

        // Act & Assert
        mockMvc.perform(post("/api/invitations/{invitationId}/accept", invitationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(invitationService, times(1)).acceptInvitation(invitationId, userId, email);
    }

    @Test
    @DisplayName("POST /api/invitations/{invitationId}/accept - Should return 400 when email doesn't match")
    void acceptInvitation_WhenEmailDoesNotMatch_ShouldReturn400() throws Exception {
        // Arrange
        when(invitationService.acceptInvitation(invitationId, userId, email))
                .thenThrow(new IllegalArgumentException("This invitation is not for you"));

        // Act & Assert
        mockMvc.perform(post("/api/invitations/{invitationId}/accept", invitationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(invitationService, times(1)).acceptInvitation(invitationId, userId, email);
    }

    @Test
    @DisplayName("POST /api/invitations/{invitationId}/accept - Should return 400 when invitation not pending")
    void acceptInvitation_WhenInvitationNotPending_ShouldReturn400() throws Exception {
        // Arrange
        when(invitationService.acceptInvitation(invitationId, userId, email))
                .thenThrow(new IllegalArgumentException("This invitation is no longer pending"));

        // Act & Assert
        mockMvc.perform(post("/api/invitations/{invitationId}/accept", invitationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(invitationService, times(1)).acceptInvitation(invitationId, userId, email);
    }

    @Test
    @DisplayName("POST /api/invitations/{invitationId}/accept - Should return 400 when user already member")
    void acceptInvitation_WhenUserAlreadyMember_ShouldReturn400() throws Exception {
        // Arrange
        when(invitationService.acceptInvitation(invitationId, userId, email))
                .thenThrow(new IllegalArgumentException("You are already a member of this group"));

        // Act & Assert
        mockMvc.perform(post("/api/invitations/{invitationId}/accept", invitationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(invitationService, times(1)).acceptInvitation(invitationId, userId, email);
    }

    // ==================== declineInvitation Tests ====================

    @Test
    @DisplayName("POST /api/invitations/{invitationId}/decline - Should decline invitation and return 200")
    void declineInvitation_WhenValidRequest_ShouldReturn200() throws Exception {
        // Arrange
        testInvitation.setStatus(InvitationStatus.DECLINED);
        when(invitationService.declineInvitation(invitationId, email))
                .thenReturn(testInvitation);

        // Act & Assert
        mockMvc.perform(post("/api/invitations/{invitationId}/decline", invitationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invitationId.toString()))
                .andExpect(jsonPath("$.invitedEmail").value(email))
                .andExpect(jsonPath("$.status").value("DECLINED"));

        verify(invitationService, times(1)).declineInvitation(invitationId, email);
    }

    @Test
    @DisplayName("POST /api/invitations/{invitationId}/decline - Should return 400 when invitation not found")
    void declineInvitation_WhenInvitationNotFound_ShouldReturn400() throws Exception {
        // Arrange
        when(invitationService.declineInvitation(invitationId, email))
                .thenThrow(new IllegalArgumentException("Invitation not found"));

        // Act & Assert
        mockMvc.perform(post("/api/invitations/{invitationId}/decline", invitationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(invitationService, times(1)).declineInvitation(invitationId, email);
    }

    @Test
    @DisplayName("POST /api/invitations/{invitationId}/decline - Should return 400 when email doesn't match")
    void declineInvitation_WhenEmailDoesNotMatch_ShouldReturn400() throws Exception {
        // Arrange
        when(invitationService.declineInvitation(invitationId, email))
                .thenThrow(new IllegalArgumentException("This invitation is not for you"));

        // Act & Assert
        mockMvc.perform(post("/api/invitations/{invitationId}/decline", invitationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(invitationService, times(1)).declineInvitation(invitationId, email);
    }

    @Test
    @DisplayName("POST /api/invitations/{invitationId}/decline - Should return 400 when invitation not pending")
    void declineInvitation_WhenInvitationNotPending_ShouldReturn400() throws Exception {
        // Arrange
        when(invitationService.declineInvitation(invitationId, email))
                .thenThrow(new IllegalArgumentException("This invitation is no longer pending"));

        // Act & Assert
        mockMvc.perform(post("/api/invitations/{invitationId}/decline", invitationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(invitationService, times(1)).declineInvitation(invitationId, email);
    }
}