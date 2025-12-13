package com.jlgs.howmuchah.service;

import com.jlgs.howmuchah.dto.request.GroupCreationRequest;
import com.jlgs.howmuchah.dto.request.GroupUpdateRequest;
import com.jlgs.howmuchah.entity.Group;
import com.jlgs.howmuchah.entity.GroupMember;
import com.jlgs.howmuchah.entity.User;
import com.jlgs.howmuchah.repository.GroupMemberRepository;
import com.jlgs.howmuchah.repository.GroupRepository;
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
@DisplayName("GroupService Unit Tests")
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private GroupService groupService;

    private UUID ownerId;
    private UUID groupId;
    private User owner;
    private Group testGroup;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        groupId = UUID.randomUUID();

        owner = new User();
        owner.setId(ownerId);
        owner.setEmail("owner@example.com");
        owner.setName("Test Owner");

        testGroup = Group.builder()
                .id(groupId)
                .name("Test Group")
                .description("Test Description")
                .owner(owner)
                .build();
    }

    // ==================== createGroup Tests ====================

    @Test
    @DisplayName("createGroup - Should create group successfully when valid request")
    void createGroup_WhenValidRequest_ShouldCreateGroup() {
        // Arrange
        GroupCreationRequest request = new GroupCreationRequest("New Group", "New Description");

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(groupRepository.existsByNameAndOwnerId(request.getName(), ownerId)).thenReturn(false);
        when(groupRepository.save(any(Group.class))).thenReturn(testGroup);
        when(groupMemberRepository.save(any(GroupMember.class))).thenReturn(new GroupMember());

        // Act
        Group result = groupService.createGroup(ownerId, request);

        // Assert - Check result
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Group");
        assertThat(result.getDescription()).isEqualTo("Test Description");

        // Verify - Check interactions
        verify(userRepository, times(1)).findById(ownerId);
        verify(groupRepository, times(1)).existsByNameAndOwnerId(request.getName(), ownerId);
        verify(groupRepository, times(1)).save(any(Group.class));
        verify(groupMemberRepository, times(1)).save(any(GroupMember.class));

        // Verify the group was created with correct data
        ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(groupCaptor.capture());
        Group capturedGroup = groupCaptor.getValue();
        assertThat(capturedGroup.getName()).isEqualTo(request.getName());
        assertThat(capturedGroup.getDescription()).isEqualTo(request.getDescription());
        assertThat(capturedGroup.getOwner()).isEqualTo(owner);

        // Verify owner was added as group member
        ArgumentCaptor<GroupMember> memberCaptor = ArgumentCaptor.forClass(GroupMember.class);
        verify(groupMemberRepository).save(memberCaptor.capture());
        GroupMember capturedMember = memberCaptor.getValue();
        assertThat(capturedMember.getGroup()).isEqualTo(testGroup);
        assertThat(capturedMember.getUser()).isEqualTo(owner);
    }

    @Test
    @DisplayName("createGroup - Should throw exception when user not found")
    void createGroup_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        GroupCreationRequest request = new GroupCreationRequest("New Group", "Description");
        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> groupService.createGroup(ownerId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(userRepository, times(1)).findById(ownerId);
        verify(groupRepository, never()).save(any(Group.class));
        verify(groupMemberRepository, never()).save(any(GroupMember.class));
    }

    @Test
    @DisplayName("createGroup - Should throw exception when duplicate group name exists")
    void createGroup_WhenDuplicateGroupName_ShouldThrowException() {
        // Arrange
        GroupCreationRequest request = new GroupCreationRequest("Existing Group", "Description");

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(groupRepository.existsByNameAndOwnerId(request.getName(), ownerId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> groupService.createGroup(ownerId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You already have a group with this name");

        verify(userRepository, times(1)).findById(ownerId);
        verify(groupRepository, times(1)).existsByNameAndOwnerId(request.getName(), ownerId);
        verify(groupRepository, never()).save(any(Group.class));
        verify(groupMemberRepository, never()).save(any(GroupMember.class));
    }

    // ==================== getAllGroupsForUser Tests ====================

    @Test
    @DisplayName("getAllGroupsForUser - Should return all groups for user")
    void getAllGroupsForUser_WhenUserHasGroups_ShouldReturnGroups() {
        // Arrange
        Group group1 = Group.builder().name("Group 1").build();
        Group group2 = Group.builder().name("Group 2").build();
        List<Group> expectedGroups = Arrays.asList(group1, group2);

        when(groupRepository.findAllGroupsForUser(ownerId)).thenReturn(expectedGroups);

        // Act
        List<Group> result = groupService.getAllGroupsForUser(ownerId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(group1, group2);

        verify(groupRepository, times(1)).findAllGroupsForUser(ownerId);
    }

    @Test
    @DisplayName("getAllGroupsForUser - Should return empty list when user has no groups")
    void getAllGroupsForUser_WhenUserHasNoGroups_ShouldReturnEmptyList() {
        // Arrange
        when(groupRepository.findAllGroupsForUser(ownerId)).thenReturn(List.of());

        // Act
        List<Group> result = groupService.getAllGroupsForUser(ownerId);

        // Assert
        assertThat(result).isEmpty();

        verify(groupRepository, times(1)).findAllGroupsForUser(ownerId);
    }

    // ==================== deleteGroup Tests ====================

    @Test
    @DisplayName("deleteGroup - Should delete group when user is owner")
    void deleteGroup_WhenUserIsOwner_ShouldDeleteGroup() {
        // Arrange
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));

        // Act
        groupService.deleteGroup(groupId, ownerId);

        // Assert
        verify(groupRepository, times(1)).findById(groupId);
        verify(groupRepository, times(1)).delete(testGroup);
    }

    @Test
    @DisplayName("deleteGroup - Should throw exception when group not found")
    void deleteGroup_WhenGroupNotFound_ShouldThrowException() {
        // Arrange
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> groupService.deleteGroup(groupId, ownerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Group not found");

        verify(groupRepository, times(1)).findById(groupId);
        verify(groupRepository, never()).delete(any(Group.class));
    }

    @Test
    @DisplayName("deleteGroup - Should throw exception when user is not owner")
    void deleteGroup_WhenUserIsNotOwner_ShouldThrowException() {
        // Arrange
        UUID nonOwnerId = UUID.randomUUID();
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));

        // Act & Assert
        assertThatThrownBy(() -> groupService.deleteGroup(groupId, nonOwnerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only the group owner can delete this group");

        verify(groupRepository, times(1)).findById(groupId);
        verify(groupRepository, never()).delete(any(Group.class));
    }

    // ==================== updateGroup Tests ====================

    @Test
    @DisplayName("updateGroup - Should update group name successfully")
    void updateGroup_WhenUpdatingName_ShouldUpdateSuccessfully() {
        // Arrange
        GroupUpdateRequest request = new GroupUpdateRequest("Updated Group", null);

        when(groupRepository.findByIdWithOwner(groupId)).thenReturn(Optional.of(testGroup));
        when(groupRepository.save(any(Group.class))).thenReturn(testGroup);

        // Act
        Group result = groupService.updateGroup(groupId, ownerId, request);

        // Assert
        assertThat(result).isNotNull();
        verify(groupRepository, times(1)).findByIdWithOwner(groupId);
        verify(groupRepository, times(1)).save(testGroup);

        // Verify the group name was updated
        assertThat(testGroup.getName()).isEqualTo("Updated Group");
    }

    @Test
    @DisplayName("updateGroup - Should update group description successfully")
    void updateGroup_WhenUpdatingDescription_ShouldUpdateSuccessfully() {
        // Arrange
        GroupUpdateRequest request = new GroupUpdateRequest(null, "Updated Description");

        when(groupRepository.findByIdWithOwner(groupId)).thenReturn(Optional.of(testGroup));
        when(groupRepository.save(any(Group.class))).thenReturn(testGroup);

        // Act
        Group result = groupService.updateGroup(groupId, ownerId, request);

        // Assert
        assertThat(result).isNotNull();
        verify(groupRepository, times(1)).save(testGroup);

        // Verify the description was updated
        assertThat(testGroup.getDescription()).isEqualTo("Updated Description");
    }

    @Test
    @DisplayName("updateGroup - Should update both name and description")
    void updateGroup_WhenUpdatingBothFields_ShouldUpdateSuccessfully() {
        // Arrange
        GroupUpdateRequest request = new GroupUpdateRequest("New Name", "New Description");

        when(groupRepository.findByIdWithOwner(groupId)).thenReturn(Optional.of(testGroup));
        when(groupRepository.save(any(Group.class))).thenReturn(testGroup);

        // Act
        Group result = groupService.updateGroup(groupId, ownerId, request);

        // Assert
        assertThat(testGroup.getName()).isEqualTo("New Name");
        assertThat(testGroup.getDescription()).isEqualTo("New Description");
        verify(groupRepository, times(1)).save(testGroup);
    }

    @Test
    @DisplayName("updateGroup - Should trim whitespace from name and description")
    void updateGroup_WhenFieldsHaveWhitespace_ShouldTrimWhitespace() {
        // Arrange
        GroupUpdateRequest request = new GroupUpdateRequest("  Trimmed Name  ", "  Trimmed Desc  ");

        when(groupRepository.findByIdWithOwner(groupId)).thenReturn(Optional.of(testGroup));
        when(groupRepository.save(any(Group.class))).thenReturn(testGroup);

        // Act
        groupService.updateGroup(groupId, ownerId, request);

        // Assert
        assertThat(testGroup.getName()).isEqualTo("Trimmed Name");
        assertThat(testGroup.getDescription()).isEqualTo("Trimmed Desc");
    }

    @Test
    @DisplayName("updateGroup - Should throw exception when no fields provided")
    void updateGroup_WhenNoFieldsProvided_ShouldThrowException() {
        // Arrange
        GroupUpdateRequest request = new GroupUpdateRequest(null, null);

        when(groupRepository.findByIdWithOwner(groupId)).thenReturn(Optional.of(testGroup));

        // Act & Assert
        assertThatThrownBy(() -> groupService.updateGroup(groupId, ownerId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one field must be provided for update");

        verify(groupRepository, times(1)).findByIdWithOwner(groupId);
        verify(groupRepository, never()).save(any(Group.class));
    }

    @Test
    @DisplayName("updateGroup - Should throw exception when group not found")
    void updateGroup_WhenGroupNotFound_ShouldThrowException() {
        // Arrange
        GroupUpdateRequest request = new GroupUpdateRequest("New Name", null);

        when(groupRepository.findByIdWithOwner(groupId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> groupService.updateGroup(groupId, ownerId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Group not found");

        verify(groupRepository, times(1)).findByIdWithOwner(groupId);
        verify(groupRepository, never()).save(any(Group.class));
    }

    @Test
    @DisplayName("updateGroup - Should throw exception when user is not owner")
    void updateGroup_WhenUserIsNotOwner_ShouldThrowException() {
        // Arrange
        UUID nonOwnerId = UUID.randomUUID();
        GroupUpdateRequest request = new GroupUpdateRequest("New Name", null);

        when(groupRepository.findByIdWithOwner(groupId)).thenReturn(Optional.of(testGroup));

        // Act & Assert
        assertThatThrownBy(() -> groupService.updateGroup(groupId, nonOwnerId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only the group owner can update this group");

        verify(groupRepository, times(1)).findByIdWithOwner(groupId);
        verify(groupRepository, never()).save(any(Group.class));
    }

    @Test
    @DisplayName("updateGroup - Should throw exception when name is empty string")
    void updateGroup_WhenNameIsEmptyString_ShouldThrowException() {
        // Arrange
        GroupUpdateRequest request = new GroupUpdateRequest("   ", null);

        when(groupRepository.findByIdWithOwner(groupId)).thenReturn(Optional.of(testGroup));

        // Act & Assert
        assertThatThrownBy(() -> groupService.updateGroup(groupId, ownerId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Group name cannot be empty");

        verify(groupRepository, never()).save(any(Group.class));
    }
}