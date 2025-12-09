package com.jlgs.howmuchah.service;

import com.jlgs.howmuchah.dto.request.GroupCreationRequest;
import com.jlgs.howmuchah.dto.request.GroupUpdateRequest;
import com.jlgs.howmuchah.entity.Group;
import com.jlgs.howmuchah.entity.User;
import com.jlgs.howmuchah.repository.GroupRepository;
import com.jlgs.howmuchah.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

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

        return groupRepository.save(group);
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
            throw new IllegalArgumentException("Only the group owner can delete this group");
        }

        groupRepository.delete(group);
    }

    @Transactional
    public Group updateGroup(UUID groupId, UUID userId, GroupUpdateRequest request) {
        // Fetch the group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // Check if the user is the owner
        if (!group.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("Only the group owner can delete this group");
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
}
