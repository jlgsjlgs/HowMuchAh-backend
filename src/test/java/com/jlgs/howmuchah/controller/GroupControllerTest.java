package com.jlgs.howmuchah.controller;

import com.jlgs.howmuchah.config.TestSecurityConfig;
import com.jlgs.howmuchah.dto.request.GroupCreationRequest;
import com.jlgs.howmuchah.dto.request.GroupUpdateRequest;
import com.jlgs.howmuchah.dto.request.InvitationRequest;
import com.jlgs.howmuchah.entity.Group;
import com.jlgs.howmuchah.entity.Invitation;
import com.jlgs.howmuchah.entity.User;
import com.jlgs.howmuchah.enums.InvitationStatus;
import com.jlgs.howmuchah.service.GroupService;
import com.jlgs.howmuchah.service.InvitationService;
import com.jlgs.howmuchah.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GroupController.class)
@Import(TestSecurityConfig.class)
@DisplayName("GroupController Integration Tests")
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private GroupService groupService;

    @MockitoBean
    private InvitationService invitationService;

    private UUID userId;
    private UUID groupId;
    private String email;
    private User owner;
    private Group testGroup;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        groupId = UUID.randomUUID();
        email = "test@example.com";

        owner = new User();
        owner.setId(userId);
        owner.setEmail(email);
        owner.setName("Test User");

        testGroup = Group.builder()
                .id(groupId)
                .name("Test Group")
                .description("Test Description")
                .owner(owner)
                .build();

        // Mock JwtUtil to return test values
        when(jwtUtil.extractUserId(any(Jwt.class))).thenReturn(userId);
        when(jwtUtil.extractEmail(any(Jwt.class))).thenReturn(email);
    }

    // ==================== createGroup Tests ====================

    @Test
    @DisplayName("POST /api/groups - Should create group and return 201")
    void createGroup_WhenValidRequest_ShouldReturn201() throws Exception {
        // Arrange
        GroupCreationRequest request = new GroupCreationRequest("New Group", "Description");
        when(groupService.createGroup(eq(userId), any(GroupCreationRequest.class)))
                .thenReturn(testGroup);

        // Act & Assert
        mockMvc.perform(post("/api/groups")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(groupId.toString()))
                .andExpect(jsonPath("$.name").value("Test Group"))
                .andExpect(jsonPath("$.description").value("Test Description"));

        verify(groupService, times(1)).createGroup(eq(userId), any(GroupCreationRequest.class));
    }

    @Test
    @DisplayName("POST /api/groups - Should return 400 when service throws IllegalArgumentException")
    void createGroup_WhenDuplicateName_ShouldReturn400() throws Exception {
        // Arrange
        GroupCreationRequest request = new GroupCreationRequest("Duplicate Group", "Description");
        when(groupService.createGroup(eq(userId), any(GroupCreationRequest.class)))
                .thenThrow(new IllegalArgumentException("You already have a group with this name"));

        // Act & Assert
        mockMvc.perform(post("/api/groups")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(groupService, times(1)).createGroup(eq(userId), any(GroupCreationRequest.class));
    }

    // ==================== getAllGroups Tests ====================

    @Test
    @DisplayName("GET /api/groups - Should return all groups for user")
    void getAllGroups_WhenUserHasGroups_ShouldReturn200() throws Exception {
        // Arrange
        User owner1 = new User();
        owner1.setId(userId);
        owner1.setEmail("owner1@example.com");

        User owner2 = new User();
        owner2.setId(UUID.randomUUID());
        owner2.setEmail("owner2@example.com");

        Group group1 = Group.builder()
                .id(UUID.randomUUID())
                .name("Group 1")
                .description("Description 1")
                .owner(owner1)  // Add owner
                .build();
        Group group2 = Group.builder()
                .id(UUID.randomUUID())
                .name("Group 2")
                .description("Description 2")
                .owner(owner2)  // Add owner
                .build();
        List<Group> groups = Arrays.asList(group1, group2);

        when(groupService.getAllGroupsForUser(userId)).thenReturn(groups);

        // Act & Assert
        mockMvc.perform(get("/api/groups")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Group 1"))
                .andExpect(jsonPath("$[1].name").value("Group 2"));

        verify(groupService, times(1)).getAllGroupsForUser(userId);
    }

    @Test
    @DisplayName("GET /api/groups - Should return empty list when user has no groups")
    void getAllGroups_WhenUserHasNoGroups_ShouldReturnEmptyList() throws Exception {
        // Arrange
        when(groupService.getAllGroupsForUser(userId)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/groups")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(groupService, times(1)).getAllGroupsForUser(userId);
    }

    // ==================== deleteGroup Tests ====================

    @Test
    @DisplayName("DELETE /api/groups/{groupId} - Should delete group and return 204")
    void deleteGroup_WhenUserIsOwner_ShouldReturn204() throws Exception {
        // Arrange
        doNothing().when(groupService).deleteGroup(groupId, userId);

        // Act & Assert
        mockMvc.perform(delete("/api/groups/{groupId}", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isNoContent());

        verify(groupService, times(1)).deleteGroup(groupId, userId);
    }

    @Test
    @DisplayName("DELETE /api/groups/{groupId} - Should return 400 when user is not owner")
    void deleteGroup_WhenUserIsNotOwner_ShouldReturn400() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("Only the group owner can delete this group"))
                .when(groupService).deleteGroup(groupId, userId);

        // Act & Assert
        mockMvc.perform(delete("/api/groups/{groupId}", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(groupService, times(1)).deleteGroup(groupId, userId);
    }

    @Test
    @DisplayName("DELETE /api/groups/{groupId} - Should return 400 when group not found")
    void deleteGroup_WhenGroupNotFound_ShouldReturn400() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("Group not found"))
                .when(groupService).deleteGroup(groupId, userId);

        // Act & Assert
        mockMvc.perform(delete("/api/groups/{groupId}", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(groupService, times(1)).deleteGroup(groupId, userId);
    }

    // ==================== updateGroup Tests ====================

    @Test
    @DisplayName("PATCH /api/groups/{groupId} - Should update group and return 200")
    void updateGroup_WhenValidRequest_ShouldReturn200() throws Exception {
        // Arrange
        GroupUpdateRequest request = new GroupUpdateRequest("Updated Name", "Updated Description");
        Group updatedGroup = Group.builder()
                .id(groupId)
                .name("Updated Name")
                .description("Updated Description")
                .owner(owner)
                .build();

        when(groupService.updateGroup(eq(groupId), eq(userId), any(GroupUpdateRequest.class)))
                .thenReturn(updatedGroup);

        // Act & Assert
        mockMvc.perform(patch("/api/groups/{groupId}", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(groupId.toString()))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.description").value("Updated Description"));

        verify(groupService, times(1)).updateGroup(eq(groupId), eq(userId), any(GroupUpdateRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/groups/{groupId} - Should return 400 when user is not owner")
    void updateGroup_WhenUserIsNotOwner_ShouldReturn400() throws Exception {
        // Arrange
        GroupUpdateRequest request = new GroupUpdateRequest("Updated Name", null);
        when(groupService.updateGroup(eq(groupId), eq(userId), any(GroupUpdateRequest.class)))
                .thenThrow(new IllegalArgumentException("Only the group owner can update this group"));

        // Act & Assert
        mockMvc.perform(patch("/api/groups/{groupId}", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(groupService, times(1)).updateGroup(eq(groupId), eq(userId), any(GroupUpdateRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/groups/{groupId} - Should return 400 when no fields provided")
    void updateGroup_WhenNoFieldsProvided_ShouldReturn400() throws Exception {
        // Arrange
        GroupUpdateRequest request = new GroupUpdateRequest(null, null);
        when(groupService.updateGroup(eq(groupId), eq(userId), any(GroupUpdateRequest.class)))
                .thenThrow(new IllegalArgumentException("At least one field must be provided for update"));

        // Act & Assert
        mockMvc.perform(patch("/api/groups/{groupId}", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(groupService, times(1)).updateGroup(eq(groupId), eq(userId), any(GroupUpdateRequest.class));
    }

    // ==================== sendInvitation Tests ====================

    @Test
    @DisplayName("POST /api/groups/{groupId}/invitations - Should send invitation and return 201")
    void sendInvitation_WhenValidRequest_ShouldReturn201() throws Exception {
        // Arrange
        UUID invitationId = UUID.randomUUID();
        InvitationRequest request = new InvitationRequest("invited@example.com");

        Invitation invitation = Invitation.builder()
                .id(invitationId)
                .group(testGroup)
                .invitedEmail("invited@example.com")
                .invitedBy(owner)
                .status(InvitationStatus.PENDING)
                .build();

        when(invitationService.sendInvitation(eq(groupId), eq(userId), any(InvitationRequest.class)))
                .thenReturn(invitation);

        // Act & Assert
        mockMvc.perform(post("/api/groups/{groupId}/invitations", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(invitationId.toString()))
                .andExpect(jsonPath("$.invitedEmail").value("invited@example.com"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(invitationService, times(1)).sendInvitation(eq(groupId), eq(userId), any(InvitationRequest.class));
    }

    @Test
    @DisplayName("POST /api/groups/{groupId}/invitations - Should return 400 when user is not owner")
    void sendInvitation_WhenUserIsNotOwner_ShouldReturn400() throws Exception {
        // Arrange
        InvitationRequest request = new InvitationRequest("invited@example.com");
        when(invitationService.sendInvitation(eq(groupId), eq(userId), any(InvitationRequest.class)))
                .thenThrow(new IllegalArgumentException("Only the group owner can send invitations"));

        // Act & Assert
        mockMvc.perform(post("/api/groups/{groupId}/invitations", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(invitationService, times(1)).sendInvitation(eq(groupId), eq(userId), any(InvitationRequest.class));
    }

    @Test
    @DisplayName("POST /api/groups/{groupId}/invitations - Should return 400 when email already invited")
    void sendInvitation_WhenEmailAlreadyInvited_ShouldReturn400() throws Exception {
        // Arrange
        InvitationRequest request = new InvitationRequest("existing@example.com");
        when(invitationService.sendInvitation(eq(groupId), eq(userId), any(InvitationRequest.class)))
                .thenThrow(new IllegalArgumentException("An invitation to this email already exists for this group"));

        // Act & Assert
        mockMvc.perform(post("/api/groups/{groupId}/invitations", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(invitationService, times(1)).sendInvitation(eq(groupId), eq(userId), any(InvitationRequest.class));
    }

    // ==================== getAllInvitations Tests ====================

    @Test
    @DisplayName("GET /api/groups/{groupId}/invitations - Should return all invitations")
    void getAllInvitations_WhenInvitationsExist_ShouldReturn200() throws Exception {
        // Arrange
        Invitation inv1 = Invitation.builder()
                .id(UUID.randomUUID())
                .group(testGroup)  // Add group
                .invitedEmail("user1@example.com")
                .invitedBy(owner)  // Add invitedBy
                .status(InvitationStatus.PENDING)
                .build();
        Invitation inv2 = Invitation.builder()
                .id(UUID.randomUUID())
                .group(testGroup)  // Add group
                .invitedEmail("user2@example.com")
                .invitedBy(owner)  // Add invitedBy
                .status(InvitationStatus.ACCEPTED)
                .build();
        List<Invitation> invitations = Arrays.asList(inv1, inv2);

        when(invitationService.getAllInvitationsForGroup(groupId, userId)).thenReturn(invitations);

        // Act & Assert
        mockMvc.perform(get("/api/groups/{groupId}/invitations", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].invitedEmail").value("user1@example.com"))
                .andExpect(jsonPath("$[1].invitedEmail").value("user2@example.com"));

        verify(invitationService, times(1)).getAllInvitationsForGroup(groupId, userId);
    }

    @Test
    @DisplayName("GET /api/groups/{groupId}/invitations - Should return 400 when user is not owner")
    void getAllInvitations_WhenUserIsNotOwner_ShouldReturn400() throws Exception {
        // Arrange
        when(invitationService.getAllInvitationsForGroup(groupId, userId))
                .thenThrow(new IllegalArgumentException("Only the group owner can view invitations"));

        // Act & Assert
        mockMvc.perform(get("/api/groups/{groupId}/invitations", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(invitationService, times(1)).getAllInvitationsForGroup(groupId, userId);
    }

    // ==================== revokeInvitation Tests ====================

    @Test
    @DisplayName("POST /api/groups/{groupId}/invitations/{invitationId} - Should revoke invitation and return 204")
    void revokeInvitation_WhenValidRequest_ShouldReturn204() throws Exception {
        // Arrange
        UUID invitationId = UUID.randomUUID();
        doNothing().when(invitationService).revokeInvitation(groupId, invitationId, userId);

        // Act & Assert
        mockMvc.perform(post("/api/groups/{groupId}/invitations/{invitationId}", groupId, invitationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isNoContent());

        verify(invitationService, times(1)).revokeInvitation(groupId, invitationId, userId);
    }

    @Test
    @DisplayName("POST /api/groups/{groupId}/invitations/{invitationId} - Should return 400 when user is not owner")
    void revokeInvitation_WhenUserIsNotOwner_ShouldReturn400() throws Exception {
        // Arrange
        UUID invitationId = UUID.randomUUID();
        doThrow(new IllegalArgumentException("Only the group owner can revoke invitations"))
                .when(invitationService).revokeInvitation(groupId, invitationId, userId);

        // Act & Assert
        mockMvc.perform(post("/api/groups/{groupId}/invitations/{invitationId}", groupId, invitationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(invitationService, times(1)).revokeInvitation(groupId, invitationId, userId);
    }

    @Test
    @DisplayName("POST /api/groups/{groupId}/invitations/{invitationId} - Should return 400 when invitation not pending")
    void revokeInvitation_WhenInvitationNotPending_ShouldReturn400() throws Exception {
        // Arrange
        UUID invitationId = UUID.randomUUID();
        doThrow(new IllegalArgumentException("Only pending invitations can be revoked"))
                .when(invitationService).revokeInvitation(groupId, invitationId, userId);

        // Act & Assert
        mockMvc.perform(post("/api/groups/{groupId}/invitations/{invitationId}", groupId, invitationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN))
                .andExpect(status().isBadRequest());

        verify(invitationService, times(1)).revokeInvitation(groupId, invitationId, userId);
    }
}