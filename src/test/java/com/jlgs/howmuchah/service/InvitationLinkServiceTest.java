package com.jlgs.howmuchah.service;

import com.jlgs.howmuchah.config.BaseUrlProperties;
import com.jlgs.howmuchah.dto.request.ClaimLinkRequest;
import com.jlgs.howmuchah.dto.response.InvitationLinkResponse;
import com.jlgs.howmuchah.dto.response.InvitationResponse;
import com.jlgs.howmuchah.dto.response.ValidateLinkResponse;
import com.jlgs.howmuchah.entity.*;
import com.jlgs.howmuchah.enums.InvitationLinkStatus;
import com.jlgs.howmuchah.enums.InvitationStatus;
import com.jlgs.howmuchah.exception.InvalidInvitationLinkException;
import com.jlgs.howmuchah.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvitationLinkService Unit Tests")
class InvitationLinkServiceTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private InvitationLinkRepository invitationLinkRepository;
    @Mock
    private InvitationRepository invitationRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private WhitelistRepository whitelistRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BaseUrlProperties baseUrlProperties;

    @InjectMocks
    private InvitationLinkService invitationLinkService;

    @Captor
    private ArgumentCaptor<Invitation> invitationCaptor;

    private UUID requesterId;
    private UUID groupId;
    private UUID userId;
    private User testUser;
    private Group testGroup;
    private InvitationLink validLink;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        requesterId = UUID.randomUUID();
        groupId = UUID.randomUUID();
        userId = UUID.randomUUID();
        baseUrl = "http://localhost:3000";

        testUser = createUser("Test User", "test@example.com");
        testGroup = createGroup("Test Group");

        validLink = InvitationLink.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .createdBy(testUser)
                .token("test-token-123")
                .maxUses(5)
                .currentUses(0)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .status(InvitationLinkStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        ReflectionTestUtils.setField(invitationLinkService, "entityManager", entityManager);
    }

    // ==================== getCurrentOrGenerateLink Tests ====================

    @Test
    @DisplayName("getCurrentOrGenerateLink - Should return existing valid link")
    void getCurrentOrGenerateLink_WhenLinkValid_ShouldReturnExisting() {
        // Arrange
        stubBaseUrl();
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, requesterId)).thenReturn(true);
        when(invitationLinkRepository.findFirstByGroup_IdAndStatusOrderByCreatedAtDesc(
                groupId, InvitationLinkStatus.ACTIVE))
                .thenReturn(Optional.of(validLink));

        // Act
        InvitationLinkResponse response = invitationLinkService.getCurrentOrGenerateLink(
                requesterId, groupId, userId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(validLink.getId());
        assertThat(response.getToken()).isEqualTo(validLink.getToken());
        assertThat(response.getCurrentUses()).isEqualTo(0);
        assertThat(response.getMaxUses()).isEqualTo(5);
        verify(invitationLinkRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("getCurrentOrGenerateLink - Should expire old link and generate new when expired")
    void getCurrentOrGenerateLink_WhenLinkExpired_ShouldGenerateNew() {
        // Arrange
        stubBaseUrl();
        InvitationLink expiredLink = InvitationLink.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .createdBy(testUser)
                .token("expired-token")
                .maxUses(5)
                .currentUses(0)
                .expiresAt(LocalDateTime.now().minusDays(1)) // Expired yesterday
                .status(InvitationLinkStatus.ACTIVE)
                .build();

        InvitationLink newLink = InvitationLink.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .createdBy(testUser)
                .token("new-token")
                .maxUses(5)
                .currentUses(0)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .status(InvitationLinkStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, requesterId)).thenReturn(true);
        when(invitationLinkRepository.findFirstByGroup_IdAndStatusOrderByCreatedAtDesc(
                groupId, InvitationLinkStatus.ACTIVE))
                .thenReturn(Optional.of(expiredLink));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(invitationLinkRepository.countByGroup_IdAndCreatedAtAfter(eq(groupId), any()))
                .thenReturn(0L);
        when(invitationLinkRepository.saveAndFlush(any())).thenReturn(newLink);

        // Act
        InvitationLinkResponse response = invitationLinkService.getCurrentOrGenerateLink(
                requesterId, groupId, userId);

        // Assert
        assertThat(response).isNotNull();
        verify(invitationLinkRepository).save(expiredLink); // Expired link saved with EXPIRED status
        assertThat(expiredLink.getStatus()).isEqualTo(InvitationLinkStatus.EXPIRED);
        verify(invitationLinkRepository).saveAndFlush(any()); // New link created
        verify(entityManager).refresh(any()); // Refreshed to get DB-generated fields
    }

    @Test
    @DisplayName("getCurrentOrGenerateLink - Should expire old link and generate new when max uses reached")
    void getCurrentOrGenerateLink_WhenMaxUsesReached_ShouldGenerateNew() {
        // Arrange
        stubBaseUrl();
        InvitationLink exhaustedLink = InvitationLink.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .createdBy(testUser)
                .token("exhausted-token")
                .maxUses(5)
                .currentUses(5) // Max uses reached
                .expiresAt(LocalDateTime.now().plusDays(7))
                .status(InvitationLinkStatus.ACTIVE)
                .build();

        InvitationLink newLink = InvitationLink.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .createdBy(testUser)
                .token("new-token")
                .maxUses(5)
                .currentUses(0)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .status(InvitationLinkStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, requesterId)).thenReturn(true);
        when(invitationLinkRepository.findFirstByGroup_IdAndStatusOrderByCreatedAtDesc(
                groupId, InvitationLinkStatus.ACTIVE))
                .thenReturn(Optional.of(exhaustedLink));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(invitationLinkRepository.countByGroup_IdAndCreatedAtAfter(eq(groupId), any()))
                .thenReturn(0L);
        when(invitationLinkRepository.saveAndFlush(any())).thenReturn(newLink);

        // Act
        InvitationLinkResponse response = invitationLinkService.getCurrentOrGenerateLink(
                requesterId, groupId, userId);

        // Assert
        assertThat(response).isNotNull();
        verify(invitationLinkRepository).save(exhaustedLink);
        assertThat(exhaustedLink.getStatus()).isEqualTo(InvitationLinkStatus.EXPIRED);
        verify(invitationLinkRepository).saveAndFlush(any());
    }

    @Test
    @DisplayName("getCurrentOrGenerateLink - Should throw when requester is not a group member")
    void getCurrentOrGenerateLink_WhenNotMember_ShouldThrow() {
        // Arrange
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, requesterId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> invitationLinkService.getCurrentOrGenerateLink(
                requesterId, groupId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only group members can generate invitation links");

        verify(invitationLinkRepository, never()).findFirstByGroup_IdAndStatusOrderByCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("getCurrentOrGenerateLink - Should throw when group not found")
    void getCurrentOrGenerateLink_WhenGroupNotFound_ShouldThrow() {
        // Arrange
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> invitationLinkService.getCurrentOrGenerateLink(
                requesterId, groupId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Group not found");

        verify(groupMemberRepository, never()).existsByGroupIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("getCurrentOrGenerateLink - Should generate new link when no active link exists")
    void getCurrentOrGenerateLink_WhenNoActiveLink_ShouldGenerateNew() {
        // Arrange
        stubBaseUrl();
        InvitationLink newLink = InvitationLink.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .createdBy(testUser)
                .token("new-token")
                .maxUses(5)
                .currentUses(0)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .status(InvitationLinkStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, requesterId)).thenReturn(true);
        when(invitationLinkRepository.findFirstByGroup_IdAndStatusOrderByCreatedAtDesc(
                groupId, InvitationLinkStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(invitationLinkRepository.countByGroup_IdAndCreatedAtAfter(eq(groupId), any()))
                .thenReturn(0L);
        when(invitationLinkRepository.saveAndFlush(any())).thenReturn(newLink);

        // Act
        InvitationLinkResponse response = invitationLinkService.getCurrentOrGenerateLink(
                requesterId, groupId, userId);

        // Assert
        assertThat(response).isNotNull();
        verify(invitationLinkRepository).saveAndFlush(any());
        verify(entityManager).refresh(any());
    }

    @Test
    @DisplayName("getCurrentOrGenerateLink - Should throw when monthly rate limit exceeded")
    void getCurrentOrGenerateLink_WhenRateLimitExceeded_ShouldThrow() {
        // Arrange
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, requesterId)).thenReturn(true);
        when(invitationLinkRepository.findFirstByGroup_IdAndStatusOrderByCreatedAtDesc(
                groupId, InvitationLinkStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(invitationLinkRepository.countByGroup_IdAndCreatedAtAfter(eq(groupId), any()))
                .thenReturn(3L); // Already 3 links this month

        // Act & Assert
        assertThatThrownBy(() -> invitationLinkService.getCurrentOrGenerateLink(
                requesterId, groupId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Maximum of 3 invitation links per group per month exceeded");

        verify(invitationLinkRepository, never()).saveAndFlush(any());
    }

    // ==================== regenerateLink Tests ====================

    @Test
    @DisplayName("regenerateLink - Should expire current link and generate new one")
    void regenerateLink_ShouldExpireAndGenerate() {
        // Arrange
        stubBaseUrl();
        InvitationLink newLink = InvitationLink.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .createdBy(testUser)
                .token("new-token")
                .maxUses(5)
                .currentUses(0)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .status(InvitationLinkStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, requesterId)).thenReturn(true);
        when(invitationLinkRepository.findFirstByGroup_IdAndStatusOrderByCreatedAtDesc(
                groupId, InvitationLinkStatus.ACTIVE))
                .thenReturn(Optional.of(validLink));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(invitationLinkRepository.countByGroup_IdAndCreatedAtAfter(eq(groupId), any()))
                .thenReturn(0L);
        when(invitationLinkRepository.saveAndFlush(any())).thenReturn(newLink);

        // Act
        InvitationLinkResponse response = invitationLinkService.regenerateLink(
                requesterId, groupId, userId);

        // Assert
        assertThat(response).isNotNull();
        verify(invitationLinkRepository).save(validLink);
        assertThat(validLink.getStatus()).isEqualTo(InvitationLinkStatus.EXPIRED);
        verify(invitationLinkRepository).saveAndFlush(any());
    }

    @Test
    @DisplayName("regenerateLink - Should work when no active link exists")
    void regenerateLink_WhenNoActiveLink_ShouldGenerateNew() {
        // Arrange
        stubBaseUrl();
        InvitationLink newLink = InvitationLink.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .createdBy(testUser)
                .token("new-token")
                .maxUses(5)
                .currentUses(0)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .status(InvitationLinkStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, requesterId)).thenReturn(true);
        when(invitationLinkRepository.findFirstByGroup_IdAndStatusOrderByCreatedAtDesc(
                groupId, InvitationLinkStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(invitationLinkRepository.countByGroup_IdAndCreatedAtAfter(eq(groupId), any()))
                .thenReturn(0L);
        when(invitationLinkRepository.saveAndFlush(any())).thenReturn(newLink);

        // Act
        InvitationLinkResponse response = invitationLinkService.regenerateLink(
                requesterId, groupId, userId);

        // Assert
        assertThat(response).isNotNull();
        verify(invitationLinkRepository, never()).save(any()); // No existing link to expire
        verify(invitationLinkRepository).saveAndFlush(any()); // New link created
    }

    // ==================== validateLink Tests ====================

    @Test
    @DisplayName("validateLink - Should return valid response for valid link")
    void validateLink_WhenValid_ShouldReturnValidResponse() {
        // Arrange
        when(invitationLinkRepository.findByIdAndToken(validLink.getId(), validLink.getToken()))
                .thenReturn(Optional.of(validLink));

        // Act
        ValidateLinkResponse response = invitationLinkService.validateLink(
                validLink.getId(), validLink.getToken());

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getValid()).isTrue();
        assertThat(response.getLinkDetails()).isNotNull();
        assertThat(response.getLinkDetails().getLinkId()).isEqualTo(validLink.getId());
        assertThat(response.getLinkDetails().getGroupId()).isEqualTo(testGroup.getId());
    }

    @Test
    @DisplayName("validateLink - Should throw when link not found")
    void validateLink_WhenNotFound_ShouldThrow() {
        // Arrange
        UUID linkId = UUID.randomUUID();
        String token = "invalid-token";
        when(invitationLinkRepository.findByIdAndToken(linkId, token))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> invitationLinkService.validateLink(linkId, token))
                .isInstanceOf(InvalidInvitationLinkException.class);
    }

    @Test
    @DisplayName("validateLink - Should throw when link expired")
    void validateLink_WhenExpired_ShouldThrow() {
        // Arrange
        InvitationLink expiredLink = InvitationLink.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .createdBy(testUser)
                .token("expired-token")
                .maxUses(5)
                .currentUses(0)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .status(InvitationLinkStatus.ACTIVE)
                .build();

        when(invitationLinkRepository.findByIdAndToken(expiredLink.getId(), expiredLink.getToken()))
                .thenReturn(Optional.of(expiredLink));

        // Act & Assert
        assertThatThrownBy(() -> invitationLinkService.validateLink(
                expiredLink.getId(), expiredLink.getToken()))
                .isInstanceOf(InvalidInvitationLinkException.class);
    }

    @Test
    @DisplayName("validateLink - Should throw when link exhausted")
    void validateLink_WhenExhausted_ShouldThrow() {
        // Arrange
        InvitationLink exhaustedLink = InvitationLink.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .createdBy(testUser)
                .token("exhausted-token")
                .maxUses(5)
                .currentUses(5)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .status(InvitationLinkStatus.ACTIVE)
                .build();

        when(invitationLinkRepository.findByIdAndToken(exhaustedLink.getId(), exhaustedLink.getToken()))
                .thenReturn(Optional.of(exhaustedLink));

        // Act & Assert
        assertThatThrownBy(() -> invitationLinkService.validateLink(
                exhaustedLink.getId(), exhaustedLink.getToken()))
                .isInstanceOf(InvalidInvitationLinkException.class);
    }

    @Test
    @DisplayName("validateLink - Should throw when link status is EXPIRED")
    void validateLink_WhenStatusExpired_ShouldThrow() {
        // Arrange
        InvitationLink expiredStatusLink = InvitationLink.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .createdBy(testUser)
                .token("token")
                .maxUses(5)
                .currentUses(0)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .status(InvitationLinkStatus.EXPIRED)
                .build();

        when(invitationLinkRepository.findByIdAndToken(expiredStatusLink.getId(), expiredStatusLink.getToken()))
                .thenReturn(Optional.of(expiredStatusLink));

        // Act & Assert
        assertThatThrownBy(() -> invitationLinkService.validateLink(
                expiredStatusLink.getId(), expiredStatusLink.getToken()))
                .isInstanceOf(InvalidInvitationLinkException.class);
    }

    // ==================== claimLink Tests ====================

    @Test
    @DisplayName("claimLink - Should create invitation and increment counter successfully")
    void claimLink_WhenValid_ShouldCreateInvitation() {
        // Arrange
        String email = "newuser@example.com";
        ClaimLinkRequest request = new ClaimLinkRequest(
                validLink.getId(), validLink.getToken(), email);

        Invitation savedInvitation = Invitation.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .invitedEmail(email)
                .invitedBy(testUser)
                .invitationLink(validLink)
                .status(InvitationStatus.PENDING)
                .build();

        when(invitationLinkRepository.findByIdAndToken(validLink.getId(), validLink.getToken()))
                .thenReturn(Optional.of(validLink));
        when(invitationRepository.findByGroup_IdAndInvitedEmail(testGroup.getId(), email))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(whitelistRepository.existsByEmail(email)).thenReturn(false);
        when(invitationRepository.save(any())).thenReturn(savedInvitation);

        // Act
        InvitationResponse response = invitationLinkService.claimLink(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getInvitedEmail()).isEqualTo(email);
        assertThat(response.getStatus()).isEqualTo(InvitationStatus.PENDING);

        verify(invitationRepository).save(invitationCaptor.capture());
        Invitation capturedInvitation = invitationCaptor.getValue();
        assertThat(capturedInvitation.getInvitedEmail()).isEqualTo(email);
        assertThat(capturedInvitation.getInvitationLink()).isEqualTo(validLink);

        verify(invitationLinkRepository).save(validLink);
        assertThat(validLink.getCurrentUses()).isEqualTo(1); // Incremented

        verify(whitelistRepository).save(any(Whitelist.class));
    }

    @Test
    @DisplayName("claimLink - Should throw when link not found")
    void claimLink_WhenLinkNotFound_ShouldThrow() {
        // Arrange
        ClaimLinkRequest request = new ClaimLinkRequest(
                UUID.randomUUID(), "invalid-token", "test@example.com");

        when(invitationLinkRepository.findByIdAndToken(request.getLinkId(), request.getToken()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> invitationLinkService.claimLink(request))
                .isInstanceOf(InvalidInvitationLinkException.class);

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("claimLink - Should throw when link expired")
    void claimLink_WhenLinkExpired_ShouldThrow() {
        // Arrange
        InvitationLink expiredLink = InvitationLink.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .createdBy(testUser)
                .token("expired-token")
                .maxUses(5)
                .currentUses(0)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .status(InvitationLinkStatus.ACTIVE)
                .build();

        ClaimLinkRequest request = new ClaimLinkRequest(
                expiredLink.getId(), expiredLink.getToken(), "test@example.com");

        when(invitationLinkRepository.findByIdAndToken(expiredLink.getId(), expiredLink.getToken()))
                .thenReturn(Optional.of(expiredLink));

        // Act & Assert
        assertThatThrownBy(() -> invitationLinkService.claimLink(request))
                .isInstanceOf(InvalidInvitationLinkException.class);

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("claimLink - Should throw when email already has invitation")
    void claimLink_WhenEmailAlreadyInvited_ShouldThrow() {
        // Arrange
        String email = "existing@example.com";
        ClaimLinkRequest request = new ClaimLinkRequest(
                validLink.getId(), validLink.getToken(), email);

        Invitation existingInvitation = Invitation.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .invitedEmail(email)
                .status(InvitationStatus.PENDING)
                .build();

        when(invitationLinkRepository.findByIdAndToken(validLink.getId(), validLink.getToken()))
                .thenReturn(Optional.of(validLink));
        when(invitationRepository.findByGroup_IdAndInvitedEmail(testGroup.getId(), email))
                .thenReturn(Optional.of(existingInvitation));

        // Act & Assert
        assertThatThrownBy(() -> invitationLinkService.claimLink(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("An invitation to the group already exists for this email");

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("claimLink - Should throw when user already a member")
    void claimLink_WhenUserAlreadyMember_ShouldThrow() {
        // Arrange
        String email = "member@example.com";
        User existingUser = createUser("Existing Member", email);
        ClaimLinkRequest request = new ClaimLinkRequest(
                validLink.getId(), validLink.getToken(), email);

        when(invitationLinkRepository.findByIdAndToken(validLink.getId(), validLink.getToken()))
                .thenReturn(Optional.of(validLink));
        when(invitationRepository.findByGroup_IdAndInvitedEmail(testGroup.getId(), email))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
        when(groupMemberRepository.existsByGroupIdAndUserId(testGroup.getId(), existingUser.getId()))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> invitationLinkService.claimLink(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You are already a member of this group");

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("claimLink - Should add email to whitelist if not present")
    void claimLink_ShouldAddToWhitelist() {
        // Arrange
        String email = "newuser@example.com";
        ClaimLinkRequest request = new ClaimLinkRequest(
                validLink.getId(), validLink.getToken(), email);

        Invitation savedInvitation = Invitation.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .invitedEmail(email)
                .invitedBy(testUser)
                .invitationLink(validLink)
                .status(InvitationStatus.PENDING)
                .build();

        when(invitationLinkRepository.findByIdAndToken(validLink.getId(), validLink.getToken()))
                .thenReturn(Optional.of(validLink));
        when(invitationRepository.findByGroup_IdAndInvitedEmail(testGroup.getId(), email))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(whitelistRepository.existsByEmail(email)).thenReturn(false);
        when(invitationRepository.save(any())).thenReturn(savedInvitation);

        // Act
        invitationLinkService.claimLink(request);

        // Assert
        verify(whitelistRepository).save(any(Whitelist.class));
    }

    @Test
    @DisplayName("claimLink - Should not add to whitelist if already present")
    void claimLink_WhenAlreadyWhitelisted_ShouldNotAddAgain() {
        // Arrange
        String email = "whitelisted@example.com";
        ClaimLinkRequest request = new ClaimLinkRequest(
                validLink.getId(), validLink.getToken(), email);

        Invitation savedInvitation = Invitation.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .invitedEmail(email)
                .invitedBy(testUser)
                .invitationLink(validLink)
                .status(InvitationStatus.PENDING)
                .build();

        when(invitationLinkRepository.findByIdAndToken(validLink.getId(), validLink.getToken()))
                .thenReturn(Optional.of(validLink));
        when(invitationRepository.findByGroup_IdAndInvitedEmail(testGroup.getId(), email))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(whitelistRepository.existsByEmail(email)).thenReturn(true);
        when(invitationRepository.save(any())).thenReturn(savedInvitation);

        // Act
        invitationLinkService.claimLink(request);

        // Assert
        verify(whitelistRepository, never()).save(any());
    }

    // ==================== Helper Methods ====================

    private void stubBaseUrl() {
        when(baseUrlProperties.getBaseUrl()).thenReturn(baseUrl);
    }

    private User createUser(String name, String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName(name);
        user.setEmail(email);
        return user;
    }

    private Group createGroup(String name) {
        Group group = new Group();
        group.setId(groupId);
        group.setName(name);
        group.setOwner(testUser);
        return group;
    }
}