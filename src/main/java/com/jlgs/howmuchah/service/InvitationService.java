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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Transactional
    public Invitation sendInvitation(UUID groupId, UUID userId, InvitationRequest request) {
        // Fetch the group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // Check if user is the group owner
        if (!group.getOwner().getId().equals(userId)) {
            log.warn("User {} attempted to maliciously invite to group {}", Encode.forJava(String.valueOf(userId)), Encode.forJava(String.valueOf(groupId)));
            throw new IllegalArgumentException("Only the group owner can send invitations");
        }

        // Fetch the inviting user
        User invitedBy = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if invitation already exists for this email in this group
        // No new invitation will be sent if status is PENDING || ACCEPTED || DECLINED
        List<Invitation> existingInvitations = invitationRepository.findByGroupId(groupId);
        boolean alreadyInvited = existingInvitations.stream()
                .anyMatch(inv -> inv.getInvitedEmail().equals(request.getInvitedEmail()));

        if (alreadyInvited) {
            throw new IllegalArgumentException("An invitation to this email already exists for this group");
        }

        log.info("Creating invitation for {} to group {} - Initiated by {}",
                Encode.forJava(request.getInvitedEmail()), Encode.forJava(String.valueOf(groupId)), Encode.forJava(invitedBy.getEmail()));

        // Create and save invitation
        Invitation invitation = Invitation.builder()
                .group(group)
                .invitedEmail(request.getInvitedEmail())
                .invitedBy(invitedBy)
                .status(InvitationStatus.PENDING)
                .build();

        return invitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public List<Invitation> getAllInvitationsForGroup(UUID groupId, UUID userId) {
        // Fetch the group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // Check if user is the group owner
        if (!group.getOwner().getId().equals(userId)) {
            log.warn("User {} attempted to maliciously fetch all invitations to group {}",
                    Encode.forJava(String.valueOf(userId)), Encode.forJava(String.valueOf(groupId)));
            throw new IllegalArgumentException("Only the group owner can view invitations");
        }

        // Fetch all invitations with details (using JOIN FETCH to avoid N+1 queries)
        return invitationRepository.findByGroupIdWithDetails(groupId);
    }

    @Transactional
    public void revokeInvitation(UUID groupId, UUID invitationId, UUID userId) {
        // Fetch the group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // Check if user is the group owner
        if (!group.getOwner().getId().equals(userId)) {
            log.warn("User {} attempted to maliciously revoke invitations to group {}",
                    Encode.forJava(String.valueOf(userId)), Encode.forJava(String.valueOf(groupId)));
            throw new IllegalArgumentException("Only the group owner can revoke invitations");
        }

        // Fetch the invitation
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));

        // Verify invitation belongs to this group
        if (!invitation.getGroup().getId().equals(groupId)) {
            throw new IllegalArgumentException("Invitation does not belong to this group");
        }

        // Check if invitation can be revoked (must be PENDING)
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Only pending invitations can be revoked");
        }

        log.info("Attempting to revoke invitation {} belonging to {} for group {}",
                Encode.forJava(String.valueOf(invitationId)), Encode.forJava(invitation.getInvitedEmail()), Encode.forJava(String.valueOf(groupId)));

        // Update status to REVOKED
        invitation.setStatus(InvitationStatus.REVOKED);
        invitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public List<Invitation> getPendingInvitationsForEmail(String email) {
        return invitationRepository.findByInvitedEmailAndStatus(email, InvitationStatus.PENDING);
    }

    @Transactional
    public Invitation acceptInvitation(UUID invitationId, UUID userId, String userEmail) {

        Invitation invitation = invitationRepository.findByIdWithDetails(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));

        // Verify if invitation is meant for this user
        if (!invitation.getInvitedEmail().equalsIgnoreCase(userEmail)) {
            log.warn("User {} maliciously attempted to accept invitation {} meant for {}",
                    Encode.forJava(userEmail), invitationId, Encode.forJava(invitation.getInvitedEmail()));
            throw new IllegalArgumentException("This invitation is not for you");
        }

        // Verify if invitation is PENDING status
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            log.warn("Attempted to accept non-pending invitation {} with status {}",
                    invitationId, invitation.getStatus());
            throw new IllegalArgumentException("This invitation is no longer pending");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean alreadyMember = groupMemberRepository.existsByGroupIdAndUserId(
                invitation.getGroup().getId(), userId);

        if (alreadyMember) {
            log.warn("User {} is already a member of group {}",
                    Encode.forJava(userEmail), invitation.getGroup().getId());
            throw new IllegalArgumentException("You are already a member of this group");
        }

        // Add user to group
        GroupMember member = GroupMember.builder()
                .group(invitation.getGroup())
                .user(user)
                .build();

        groupMemberRepository.save(member);

        // Update invitation status
        invitation.setStatus(InvitationStatus.ACCEPTED);
        return invitationRepository.save(invitation);
    }

    @Transactional public Invitation declineInvitation(UUID invitationId, String userEmail) {

        Invitation invitation = invitationRepository.findByIdWithDetails(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));

        // Verify if invitation is meant for this user
        if (!invitation.getInvitedEmail().equalsIgnoreCase(userEmail)) {
            log.warn("User {} maliciously attempted to decline invitation {} meant for {}",
                    Encode.forJava(userEmail), invitationId, Encode.forJava(invitation.getInvitedEmail()));
            throw new IllegalArgumentException("This invitation is not for you");
        }

        // Verify if invitation is PENDING status
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            log.warn("Attempted to decline non-pending invitation {} with status {}",
                    invitationId, invitation.getStatus());
            throw new IllegalArgumentException("This invitation is no longer pending");
        }

        // Update invitation status
        invitation.setStatus(InvitationStatus.DECLINED);
        return invitationRepository.save(invitation);
    }
}