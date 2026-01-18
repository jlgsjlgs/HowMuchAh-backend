package com.jlgs.howmuchah.service;

import com.jlgs.howmuchah.config.BaseUrlProperties;
import com.jlgs.howmuchah.dto.request.ClaimLinkRequest;
import com.jlgs.howmuchah.dto.response.InvitationLinkResponse;
import com.jlgs.howmuchah.dto.response.InvitationResponse;
import com.jlgs.howmuchah.dto.response.LinkDetailsResponse;
import com.jlgs.howmuchah.dto.response.ValidateLinkResponse;
import com.jlgs.howmuchah.entity.*;
import com.jlgs.howmuchah.enums.InvitationLinkStatus;
import com.jlgs.howmuchah.enums.InvitationStatus;
import com.jlgs.howmuchah.exception.InvalidInvitationLinkException;
import com.jlgs.howmuchah.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationLinkService {

    private static final int MAX_LINKS_PER_GROUP_PER_MONTH = 3;

    private final InvitationLinkRepository invitationLinkRepository;
    private final InvitationRepository invitationRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final WhitelistRepository whitelistRepository;
    private final UserRepository userRepository;
    private final BaseUrlProperties baseUrlProperties;

    @Transactional
    public InvitationLinkResponse getCurrentOrGenerateLink(UUID requester, UUID groupId, UUID userId) {
        // Validate group exists
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Check if requester is part of the group
        if (!groupMemberRepository.existsByGroupIdAndUserId(group.getId(), requester)) {
            log.warn("User {} attempted to maliciously get/generate invitation link for group {}",
                    Encode.forJava(String.valueOf(requester)), Encode.forJava(String.valueOf(group.getId())));
            throw new IllegalArgumentException("Only group members can generate invitation links");
        }

        // Find current active link
        Optional<InvitationLink> activeLink = invitationLinkRepository
                .findFirstByGroup_IdAndStatusOrderByCreatedAtDesc(
                        groupId,
                        InvitationLinkStatus.ACTIVE
                );

        if (activeLink.isPresent()) {
            InvitationLink link = activeLink.get();

            // Check if link is still valid
            if (isLinkInvalid(link)) {
                // Mark as expired
                link.setStatus(InvitationLinkStatus.EXPIRED);
                invitationLinkRepository.save(link);
                log.info("Auto-expired link {} for group {}", link.getId(), groupId);

                // Generate new link (fall through)
            } else {
                // Still valid, return it
                return InvitationLinkResponse.fromInvitationLink(
                        link,
                        baseUrlProperties.getBaseUrl()
                );
            }
        }

        // No active link or previous one expired → generate new
        return generateNewLink(groupId, userId);
    }

    @Transactional
    public InvitationLinkResponse regenerateLink(UUID requester, UUID groupId, UUID userId) {
        // Validate group exists
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Check if requester is part of the group
        if (!groupMemberRepository.existsByGroupIdAndUserId(group.getId(), requester)) {
            log.warn("User {} attempted to maliciously regenerate invitation link for group {}",
                    Encode.forJava(String.valueOf(requester)), Encode.forJava(String.valueOf(group.getId())));
            throw new IllegalArgumentException("Only group members can regenerate invitation links");
        }

        // Mark any existing active link as expired
        Optional<InvitationLink> activeLink = invitationLinkRepository
                .findFirstByGroup_IdAndStatusOrderByCreatedAtDesc(
                        groupId,
                        InvitationLinkStatus.ACTIVE
                );

        activeLink.ifPresent(link -> {
            link.setStatus(InvitationLinkStatus.EXPIRED);
            invitationLinkRepository.save(link);
            log.info("Manually expired link {} for group {}", link.getId(), groupId);
        });

        // Generate new link
        return generateNewLink(groupId, userId);
    }

    @Transactional(readOnly = true)
    public ValidateLinkResponse validateLink(UUID linkId, String token) {
        // Find link by id and token
        Optional<InvitationLink> linkOpt = invitationLinkRepository.findByIdAndToken(linkId, token);

        if (linkOpt.isEmpty()) {
            throw new InvalidInvitationLinkException();
        }

        InvitationLink link = linkOpt.get();

        // Check if link is valid
        if (isLinkInvalid(link)) {
            throw new InvalidInvitationLinkException();
        }

        // Return valid response with details
        LinkDetailsResponse details = LinkDetailsResponse.fromInvitationLink(link);
        return ValidateLinkResponse.valid(details);
    }

    @Transactional
    public InvitationResponse claimLink(ClaimLinkRequest request) {
        // 1. Validate link
        Optional<InvitationLink> linkOpt = invitationLinkRepository
                .findByIdAndToken(request.getLinkId(), request.getToken());

        if (linkOpt.isEmpty()) {
            throw new InvalidInvitationLinkException();
        }

        InvitationLink link = linkOpt.get();

        // 2. Check if link is still valid
        if (isLinkInvalid(link)) {
            throw new InvalidInvitationLinkException();
        }

        // 3. Check if email already has invitation for this group
        Optional<Invitation> existingInvitation = invitationRepository
                .findByGroup_IdAndInvitedEmail(link.getGroup().getId(), request.getEmail());

        if (existingInvitation.isPresent()) {
            throw new IllegalArgumentException("An invitation to the group already exists for this email");
        }

        // 4. Check if user with this email is already in group_members
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isPresent()) {
            boolean isMember = groupMemberRepository.existsByGroupIdAndUserId(
                    link.getGroup().getId(),
                    userOpt.get().getId()
            );
            if (isMember) {
                throw new IllegalArgumentException("You are already a member of this group");
            }
        }

        // 5. Add email to whitelist (if not already there)
        addToWhitelist(request.getEmail());

        // 6. Create invitation
        Invitation invitation = Invitation.builder()
                .group(link.getGroup())
                .invitedEmail(request.getEmail())
                .invitedBy(link.getCreatedBy())
                .invitationLink(link)
                .status(InvitationStatus.PENDING)
                .build();

        invitation = invitationRepository.save(invitation);
        log.info("Created invitation {} via link {} for email {}",
                invitation.getId(), link.getId(), request.getEmail());

        // 7. Increment link usage counter (atomic update)
        link.setCurrentUses(link.getCurrentUses() + 1);
        invitationLinkRepository.save(link);

        // 8. Return created invitation
        return InvitationResponse.fromInvitation(invitation);
    }

    // ============ PRIVATE HELPER METHODS ============

    /**
     * Generate a new invitation link with rate limiting
     */
    private InvitationLinkResponse generateNewLink(UUID groupId, UUID userId) {
        // Check monthly rate limit
        enforceMonthlyRateLimit(groupId);

        // Get group and user entities
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Create new link
        InvitationLink link = InvitationLink.builder()
                .group(group)
                .createdBy(user)
                .maxUses(5)
                .currentUses(0)
                .status(InvitationLinkStatus.ACTIVE)
                .build();

        link = invitationLinkRepository.save(link);
        log.info("Generated new invitation link {} for group {}", link.getId(), groupId);

        return InvitationLinkResponse.fromInvitationLink(
                link,
                baseUrlProperties.getBaseUrl()
        );
    }

    /**
     * Check if link is invalid (expired or exhausted)
     */
    private boolean isLinkInvalid(InvitationLink link) {
        LocalDateTime now = LocalDateTime.now();
        return link.getStatus() != InvitationLinkStatus.ACTIVE
                || link.getExpiresAt().isBefore(now)
                || link.getCurrentUses() >= link.getMaxUses();
    }

    /**
     * Enforce 3 links per group per month rate limit
     */
    private void enforceMonthlyRateLimit(UUID groupId) {
        LocalDateTime monthStart = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        long linksCreatedThisMonth = invitationLinkRepository
                .countByGroup_IdAndCreatedAtAfter(groupId, monthStart);

        if (linksCreatedThisMonth >= MAX_LINKS_PER_GROUP_PER_MONTH) {
            throw new IllegalArgumentException(
                    "Maximum of " + MAX_LINKS_PER_GROUP_PER_MONTH +
                            " invitation links per group per month exceeded. Limit resets on the 1st of next month."
            );
        }
    }

    /**
     * Add email to whitelist if not already present
     */
    private void addToWhitelist(String email) {
        if (!whitelistRepository.existsByEmail(email)) {
            Whitelist whitelist = Whitelist.builder()
                    .email(email)
                    .build();
            whitelistRepository.save(whitelist);
            log.info("Added email {} to whitelist", email);
        }
    }
}