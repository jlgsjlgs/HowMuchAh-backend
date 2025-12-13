package com.jlgs.howmuchah.service;

import com.jlgs.howmuchah.dto.request.GroupCreationRequest;
import com.jlgs.howmuchah.dto.request.GroupUpdateRequest;
import com.jlgs.howmuchah.entity.Group;
import com.jlgs.howmuchah.entity.GroupMember;
import com.jlgs.howmuchah.entity.GroupMemberId;
import com.jlgs.howmuchah.entity.User;
import com.jlgs.howmuchah.repository.GroupMemberRepository;
import com.jlgs.howmuchah.repository.GroupRepository;
import com.jlgs.howmuchah.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Transactional
    public Group createGroup(UUID ownerId, GroupCreationRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check for duplicate before creating
        boolean exists = groupRepository.existsByNameAndOwnerId(request.getName(), ownerId);
        if (exists) {
            throw new IllegalArgumentException("You already have a group with this name");
        }

        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(owner)
                .build();

        Group savedGroup = groupRepository.save(group);

        GroupMember ownerMember = GroupMember.builder()
                .group(savedGroup)
                .user(owner)
                .build();

        groupMemberRepository.save(ownerMember);

        return savedGroup;
    }

    @Transactional(readOnly = true)
    public List<Group> getAllGroupsForUser(UUID userId) {
        return groupRepository.findAllGroupsForUser(userId);
    }

    @Transactional
    public void deleteGroup(UUID groupId, UUID userId) {
        // Fetch the group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // Check if the user is the owner
        if (!group.getOwner().getId().equals(userId)) {
            log.warn("User {} attempted to maliciously delete group {}", Encode.forJava(String.valueOf(userId)), Encode.forJava(String.valueOf(groupId)));
            throw new IllegalArgumentException("Only the group owner can delete this group");
        }

        groupRepository.delete(group);
    }

    @Transactional
    public Group updateGroup(UUID groupId, UUID userId, GroupUpdateRequest request) {
        // Fetch the group
        Group group = groupRepository.findByIdWithOwner(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        if (request.getName() == null && request.getDescription() == null) {
            throw new IllegalArgumentException("At least one field must be provided for update");
        }

        // Check if the user is the owner
        if (!group.getOwner().getId().equals(userId)) {
            log.warn("User {} attempted to maliciously update group {} details", Encode.forJava(String.valueOf(userId)), Encode.forJava(String.valueOf(groupId)));
            throw new IllegalArgumentException("Only the group owner can update this group");
        }

        if (request.getName() != null) {
            if (request.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Group name cannot be empty");
            }
            group.setName(request.getName().trim());
        }

        if (request.getDescription() != null) {
            group.setDescription(request.getDescription().trim());
        }

        return groupRepository.save(group);
    }

    @Transactional(readOnly = true)
    public List<GroupMember> getGroupMembers(UUID groupId, UUID requestingUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // Verify the user is a member or owner
        boolean isMember = groupMemberRepository.existsByGroupIdAndUserId(groupId, requestingUserId);
        boolean isOwner = group.getOwner().getId().equals(requestingUserId);

        if (!isMember && !isOwner) {
            throw new AccessDeniedException("You don't have access to this group");
        }

        return groupMemberRepository.findByGroupId(groupId);
    }

    @Transactional
    public void removeMember(UUID groupId, UUID userIdToRemove, UUID requestingUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // Only owner can remove members
        if (!group.getOwner().getId().equals(requestingUserId)) {
            throw new AccessDeniedException("Only the group owner can remove members");
        }

        // Can't remove the owner
        if (group.getOwner().getId().equals(userIdToRemove)) {
            throw new IllegalArgumentException("Cannot remove the group owner");
        }

        // Remove the member
        GroupMemberId memberId = new GroupMemberId(groupId, userIdToRemove);
        if (!groupMemberRepository.existsById(memberId)) {
            throw new IllegalArgumentException("User is not a member of this group");
        }

        groupMemberRepository.deleteById(memberId);
    }

    @Transactional
    public void leaveGroup(UUID groupId, UUID userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // Owner can't leave
        if (group.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("Group owner cannot leave. Delete the group instead.");
        }

        // Check if user is a member
        GroupMemberId memberId = new GroupMemberId(groupId, userId);
        if (!groupMemberRepository.existsById(memberId)) {
            throw new IllegalArgumentException("You are not a member of this group");
        }

        groupMemberRepository.deleteById(memberId);
    }
}
