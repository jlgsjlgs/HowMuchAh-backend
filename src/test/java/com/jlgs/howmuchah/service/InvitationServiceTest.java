package com.jlgs.howmuchah.service;

import com.jlgs.howmuchah.dto.request.InvitationRequest;
import com.jlgs.howmuchah.entity.Group;
import com.jlgs.howmuchah.entity.GroupMember;
import com.jlgs.howmuchah.entity.Invitation;
import com.jlgs.howmuchah.entity.User;
import com.jlgs.howmuchah.enums.InvitationStatus;
import com.jlgs.howmuchah.repository.GroupMemberRepository;
import com.jlgs.howmuchah.repository.GroupRepository;
import com.jlgs.howmuchah.repository.InvitationRepository;
import com.jlgs.howmuchah.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvitationService Unit Tests")
class InvitationServiceTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private InvitationService invitationService;

    private UUID groupId;
    private UUID userId;
    private UUID invitationId;
    private User owner;
    private User invitedUser;
    private Group group;
    private Invitation invitation;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        userId = UUID.randomUUID();
        invitationId = UUID.randomUUID();

        owner = new User();
        owner.setId(userId);
        owner.setEmail("owner@example.com");
        owner.setName("Group Owner");

        invitedUser = new User();
        invitedUser.setId(UUID.randomUUID());
        invitedUser.setEmail("invited@example.com");
        invitedUser.setName("Invited User");

        group = Group.builder()
                .id(groupId)
                .name("Test Group")
                .owner(owner)
                .build();

        invitation = Invitation.builder()
                .id(invitationId)
                .group(group)
                .invitedEmail("invited@example.com")
                .invitedBy(owner)
                .status(InvitationStatus.PENDING)
                .build();
    }

    // ==================== sendInvitation Tests ====================

    @Test
    @DisplayName("sendInvitation - Should send invitation successfully when valid request")
    void sendInvitation_WhenValidRequest_ShouldSendInvitation() {
        // Arrange
        InvitationRequest request = new InvitationRequest("newuser@example.com");

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(invitationRepository.findByGroupId(groupId)).thenReturn(List.of());
        when(invitationRepository.save(any(Invitation.class))).thenReturn(invitation);

        // Act
        Invitation result = invitationService.sendInvitation(groupId, userId, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(InvitationStatus.PENDING);

        // Verify interactions
        verify(groupRepository, times(1)).findById(groupId);
        verify(userRepository, times(1)).findById(userId);
        verify(invitationRepository, times(1)).findByGroupId(groupId);
        verify(invitationRepository, times(1)).save(any(Invitation.class));

        // Verify the invitation was created with correct data
        ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitationRepository).save(captor.capture());
        Invitation capturedInvitation = captor.getValue();
        assertThat(capturedInvitation.getGroup()).isEqualTo(group);
        assertThat(capturedInvitation.getInvitedEmail()).isEqualTo(request.getInvitedEmail());
        assertThat(capturedInvitation.getInvitedBy()).isEqualTo(owner);
        assertThat(capturedInvitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    @DisplayName("sendInvitation - Should throw exception when group not found")
    void sendInvitation_WhenGroupNotFound_ShouldThrowException() {
        // Arrange
        InvitationRequest request = new InvitationRequest("newuser@example.com");
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> invitationService.sendInvitation(groupId, userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Group not found");

        verify(groupRepository, times(1)).findById(groupId);
        verify(invitationRepository, never()).save(any(Invitation.class));
    }

    @Test
    @DisplayName("sendInvitation - Should throw exception when user is not group owner")
    void sendInvitation_WhenUserIsNotOwner_ShouldThrowException() {
        // Arrange
        UUID nonOwnerId = UUID.randomUUID();
        InvitationRequest request = new InvitationRequest("newuser@example.com");

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        // Act & Assert
        assertThatThrownBy(() -> invitationService.sendInvitation(groupId, nonOwnerId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only the group owner can send invitations");

        verify(groupRepository, times(1)).findById(groupId);
        verify(invitationRepository, never()).save(any(Invitation.class));
    }

    @Test
    @DisplayName("sendInvitation - Should throw exception when user not found")
    void sendInvitation_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        InvitationRequest request = new InvitationRequest("newuser@example.com");

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> invitationService.sendInvitation(groupId, userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");

        verify(invitationRepository, never()).save(any(Invitation.class));
    }

    @Test
    @DisplayName("sendInvitation - Should throw exception when email already invited")
    void sendInvitation_WhenEmailAlreadyInvited_ShouldThrowException() {
        // Arrange
        String existingEmail = "existing@example.com";
        InvitationRequest request = new InvitationRequest(existingEmail);

        Invitation existingInvitation = Invitation.builder()
                .invitedEmail(existingEmail)
                .status(InvitationStatus.PENDING)
                .build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(invitationRepository.findByGroupId(groupId)).thenReturn(List.of(existingInvitation));

        // Act & Assert
        assertThatThrownBy(() -> invitationService.sendInvitation(groupId, userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("An invitation to this email already exists for this group");

        verify(invitationRepository, never()).save(any(Invitation.class));
    }

    // ==================== getAllInvitationsForGroup Tests ====================

    @Test
    @DisplayName("getAllInvitationsForGroup - Should return all invitations when user is owner")
    void getAllInvitationsForGroup_WhenUserIsOwner_ShouldReturnInvitations() {
        // Arrange
        Invitation inv1 = Invitation.builder().invitedEmail("user1@example.com").build();
        Invitation inv2 = Invitation.builder().invitedEmail("user2@example.com").build();
        List<Invitation> expectedInvitations = Arrays.asList(inv1, inv2);

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(invitationRepository.findByGroupIdWithDetails(groupId)).thenReturn(expectedInvitations);

        // Act
        List<Invitation> result = invitationService.getAllInvitationsForGroup(groupId, userId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(inv1, inv2);

        verify(groupRepository, times(1)).findById(groupId);
        verify(invitationRepository, times(1)).findByGroupIdWithDetails(groupId);
    }

    @Test
    @DisplayName("getAllInvitationsForGroup - Should throw exception when group not found")
    void getAllInvitationsForGroup_WhenGroupNotFound_ShouldThrowException() {
        // Arrange
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> invitationService.getAllInvitationsForGroup(groupId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Group not found");

        verify(invitationRepository, never()).findByGroupIdWithDetails(any());
    }

    @Test
    @DisplayName("getAllInvitationsForGroup - Should throw exception when user is not owner")
    void getAllInvitationsForGroup_WhenUserIsNotOwner_ShouldThrowException() {
        // Arrange
        UUID nonOwnerId = UUID.randomUUID();
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        // Act & Assert
        assertThatThrownBy(() -> invitationService.getAllInvitationsForGroup(groupId, nonOwnerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only the group owner can view invitations");

        verify(invitationRepository, never()).findByGroupIdWithDetails(any());
    }

    // ==================== revokeInvitation Tests ====================

    @Test
    @DisplayName("revokeInvitation - Should revoke invitation successfully when valid")
    void revokeInvitation_WhenValid_ShouldRevokeInvitation() {
        // Arrange
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(Invitation.class))).thenReturn(invitation);

        // Act
        invitationService.revokeInvitation(groupId, invitationId, userId);

        // Assert
        verify(groupRepository, times(1)).findById(groupId);
        verify(invitationRepository, times(1)).findById(invitationId);
        verify(invitationRepository, times(1)).save(invitation);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.REVOKED);
    }

    @Test
    @DisplayName("revokeInvitation - Should throw exception when group not found")
    void revokeInvitation_WhenGroupNotFound_ShouldThrowException() {
        // Arrange
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> invitationService.revokeInvitation(groupId, invitationId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Group not found");

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("revokeInvitation - Should throw exception when user is not owner")
    void revokeInvitation_WhenUserIsNotOwner_ShouldThrowException() {
        // Arrange
        UUID nonOwnerId = UUID.randomUUID();
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        // Act & Assert
        assertThatThrownBy(() -> invitationService.revokeInvitation(groupId, invitationId, nonOwnerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only the group owner can revoke invitations");

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("revokeInvitation - Should throw exception when invitation not found")
    void revokeInvitation_WhenInvitationNotFound_ShouldThrowException() {
        // Arrange
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> invitationService.revokeInvitation(groupId, invitationId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invitation not found");

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("revokeInvitation - Should throw exception when invitation belongs to different group")
    void revokeInvitation_WhenInvitationBelongsToDifferentGroup_ShouldThrowException() {
        // Arrange
        UUID differentGroupId = UUID.randomUUID();
        Group differentGroup = Group.builder().id(differentGroupId).owner(owner).build();
        Invitation invitationFromDifferentGroup = Invitation.builder()
                .id(invitationId)
                .group(differentGroup)
                .status(InvitationStatus.PENDING)
                .build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitationFromDifferentGroup));

        // Act & Assert
        assertThatThrownBy(() -> invitationService.revokeInvitation(groupId, invitationId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invitation does not belong to this group");

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("revokeInvitation - Should throw exception when invitation is not pending")
    void revokeInvitation_WhenInvitationNotPending_ShouldThrowException() {
        // Arrange
        invitation.setStatus(InvitationStatus.ACCEPTED);

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        // Act & Assert
        assertThatThrownBy(() -> invitationService.revokeInvitation(groupId, invitationId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only pending invitations can be revoked");

        verify(invitationRepository, never()).save(any());
    }

    // ==================== getPendingInvitationsForEmail Tests ====================

    @Test
    @DisplayName("getPendingInvitationsForEmail - Should return pending invitations for email")
    void getPendingInvitationsForEmail_WhenInvitationsExist_ShouldReturnInvitations() {
        // Arrange
        String email = "user@example.com";
        Invitation inv1 = Invitation.builder().invitedEmail(email).status(InvitationStatus.PENDING).build();
        Invitation inv2 = Invitation.builder().invitedEmail(email).status(InvitationStatus.PENDING).build();
        List<Invitation> expectedInvitations = Arrays.asList(inv1, inv2);

        when(invitationRepository.findByInvitedEmailAndStatus(email, InvitationStatus.PENDING))
                .thenReturn(expectedInvitations);

        // Act
        List<Invitation> result = invitationService.getPendingInvitationsForEmail(email);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(inv1, inv2);

        verify(invitationRepository, times(1)).findByInvitedEmailAndStatus(email, InvitationStatus.PENDING);
    }

    @Test
    @DisplayName("getPendingInvitationsForEmail - Should return empty list when no invitations")
    void getPendingInvitationsForEmail_WhenNoInvitations_ShouldReturnEmptyList() {
        // Arrange
        String email = "user@example.com";
        when(invitationRepository.findByInvitedEmailAndStatus(email, InvitationStatus.PENDING))
                .thenReturn(List.of());

        // Act
        List<Invitation> result = invitationService.getPendingInvitationsForEmail(email);

        // Assert
        assertThat(result).isEmpty();

        verify(invitationRepository, times(1)).findByInvitedEmailAndStatus(email, InvitationStatus.PENDING);
    }

    // ==================== acceptInvitation Tests ====================

    @Test
    @DisplayName("acceptInvitation - Should accept invitation successfully")
    void acceptInvitation_WhenValid_ShouldAcceptInvitation() {
        // Arrange
        UUID invitedUserId = invitedUser.getId();
        String invitedEmail = "invited@example.com";

        when(invitationRepository.findByIdWithDetails(invitationId)).thenReturn(Optional.of(invitation));
        when(userRepository.findById(invitedUserId)).thenReturn(Optional.of(invitedUser));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, invitedUserId)).thenReturn(false);
        when(groupMemberRepository.save(any(GroupMember.class))).thenReturn(new GroupMember());
        when(invitationRepository.save(any(Invitation.class))).thenReturn(invitation);

        // Act
        Invitation result = invitationService.acceptInvitation(invitationId, invitedUserId, invitedEmail);

        // Assert
        assertThat(result).isNotNull();
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);

        verify(invitationRepository, times(1)).findByIdWithDetails(invitationId);
        verify(userRepository, times(1)).findById(invitedUserId);
        verify(groupMemberRepository, times(1)).existsByGroupIdAndUserId(groupId, invitedUserId);
        verify(groupMemberRepository, times(1)).save(any(GroupMember.class));
        verify(invitationRepository, times(1)).save(invitation);

        // Verify group member was created correctly
        ArgumentCaptor<GroupMember> captor = ArgumentCaptor.forClass(GroupMember.class);
        verify(groupMemberRepository).save(captor.capture());
        GroupMember capturedMember = captor.getValue();
        assertThat(capturedMember.getGroup()).isEqualTo(group);
        assertThat(capturedMember.getUser()).isEqualTo(invitedUser);
    }

    @Test
    @DisplayName("acceptInvitation - Should throw exception when invitation not found")
    void acceptInvitation_WhenInvitationNotFound_ShouldThrowException() {
        // Arrange
        when(invitationRepository.findByIdWithDetails(invitationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> invitationService.acceptInvitation(invitationId, userId, "user@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invitation not found");

        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("acceptInvitation - Should throw exception when email doesn't match")
    void acceptInvitation_WhenEmailDoesNotMatch_ShouldThrowException() {
        // Arrange
        String wrongEmail = "wrong@example.com";

        when(invitationRepository.findByIdWithDetails(invitationId)).thenReturn(Optional.of(invitation));

        // Act & Assert
        assertThatThrownBy(() -> invitationService.acceptInvitation(invitationId, userId, wrongEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This invitation is not for you");

        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("acceptInvitation - Should throw exception when invitation is not pending")
    void acceptInvitation_WhenInvitationNotPending_ShouldThrowException() {
        // Arrange
        invitation.setStatus(InvitationStatus.ACCEPTED);

        when(invitationRepository.findByIdWithDetails(invitationId)).thenReturn(Optional.of(invitation));

        // Act & Assert
        assertThatThrownBy(() -> invitationService.acceptInvitation(invitationId, userId, "invited@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This invitation is no longer pending");

        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("acceptInvitation - Should throw exception when user not found")
    void acceptInvitation_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        when(invitationRepository.findByIdWithDetails(invitationId)).thenReturn(Optional.of(invitation));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> invitationService.acceptInvitation(invitationId, userId, "invited@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");

        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("acceptInvitation - Should throw exception when user already member")
    void acceptInvitation_WhenUserAlreadyMember_ShouldThrowException() {
        // Arrange
        when(invitationRepository.findByIdWithDetails(invitationId)).thenReturn(Optional.of(invitation));
        when(userRepository.findById(userId)).thenReturn(Optional.of(invitedUser));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> invitationService.acceptInvitation(invitationId, userId, "invited@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You are already a member of this group");

        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("acceptInvitation - Should handle case insensitive email matching")
    void acceptInvitation_WhenEmailCaseDifferent_ShouldAcceptInvitation() {
        // Arrange
        UUID invitedUserId = invitedUser.getId();
        String invitedEmailUpperCase = "INVITED@EXAMPLE.COM";

        when(invitationRepository.findByIdWithDetails(invitationId)).thenReturn(Optional.of(invitation));
        when(userRepository.findById(invitedUserId)).thenReturn(Optional.of(invitedUser));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, invitedUserId)).thenReturn(false);
        when(groupMemberRepository.save(any(GroupMember.class))).thenReturn(new GroupMember());
        when(invitationRepository.save(any(Invitation.class))).thenReturn(invitation);

        // Act
        Invitation result = invitationService.acceptInvitation(invitationId, invitedUserId, invitedEmailUpperCase);

        // Assert
        assertThat(result).isNotNull();
        verify(groupMemberRepository, times(1)).save(any(GroupMember.class));
    }

    // ==================== declineInvitation Tests ====================

    @Test
    @DisplayName("declineInvitation - Should decline invitation successfully")
    void declineInvitation_WhenValid_ShouldDeclineInvitation() {
        // Arrange
        String invitedEmail = "invited@example.com";

        when(invitationRepository.findByIdWithDetails(invitationId)).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(Invitation.class))).thenReturn(invitation);

        // Act
        Invitation result = invitationService.declineInvitation(invitationId, invitedEmail);

        // Assert
        assertThat(result).isNotNull();
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.DECLINED);

        verify(invitationRepository, times(1)).findByIdWithDetails(invitationId);
        verify(invitationRepository, times(1)).save(invitation);
    }

    @Test
    @DisplayName("declineInvitation - Should throw exception when invitation not found")
    void declineInvitation_WhenInvitationNotFound_ShouldThrowException() {
        // Arrange
        when(invitationRepository.findByIdWithDetails(invitationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> invitationService.declineInvitation(invitationId, "user@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invitation not found");

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("declineInvitation - Should throw exception when email doesn't match")
    void declineInvitation_WhenEmailDoesNotMatch_ShouldThrowException() {
        // Arrange
        String wrongEmail = "wrong@example.com";

        when(invitationRepository.findByIdWithDetails(invitationId)).thenReturn(Optional.of(invitation));

        // Act & Assert
        assertThatThrownBy(() -> invitationService.declineInvitation(invitationId, wrongEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This invitation is not for you");

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("declineInvitation - Should throw exception when invitation is not pending")
    void declineInvitation_WhenInvitationNotPending_ShouldThrowException() {
        // Arrange
        invitation.setStatus(InvitationStatus.REVOKED);

        when(invitationRepository.findByIdWithDetails(invitationId)).thenReturn(Optional.of(invitation));

        // Act & Assert
        assertThatThrownBy(() -> invitationService.declineInvitation(invitationId, "invited@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This invitation is no longer pending");

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("declineInvitation - Should handle case insensitive email matching")
    void declineInvitation_WhenEmailCaseDifferent_ShouldDeclineInvitation() {
        // Arrange
        String invitedEmailUpperCase = "INVITED@EXAMPLE.COM";

        when(invitationRepository.findByIdWithDetails(invitationId)).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(Invitation.class))).thenReturn(invitation);

        // Act
        Invitation result = invitationService.declineInvitation(invitationId, invitedEmailUpperCase);

        // Assert
        assertThat(result).isNotNull();
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.DECLINED);
        verify(invitationRepository, times(1)).save(invitation);
    }
}