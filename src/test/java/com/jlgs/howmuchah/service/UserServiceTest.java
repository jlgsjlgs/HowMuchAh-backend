package com.jlgs.howmuchah.service;

import com.jlgs.howmuchah.entity.User;
import com.jlgs.howmuchah.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private UUID testUserId;
    private String testEmail;
    private String testName;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testEmail = "test@example.com";
        testName = "Test User";
    }

    @Test
    @DisplayName("upsertUser - Should create new user when user does not exist")
    void upsertUser_WhenUserDoesNotExist_ShouldCreateNewUser() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        User savedUser = new User();
        savedUser.setId(testUserId);
        savedUser.setEmail(testEmail);
        savedUser.setName(testName);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.upsertUser(testUserId, testEmail, testName);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUserId);
        assertThat(result.getEmail()).isEqualTo(testEmail);
        assertThat(result.getName()).isEqualTo(testName);

        // Verify repository interactions
        verify(userRepository, times(1)).findById(testUserId);
        verify(userRepository, times(1)).save(any(User.class));

        // Verify the saved user had correct values
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getId()).isEqualTo(testUserId);
        assertThat(capturedUser.getEmail()).isEqualTo(testEmail);
        assertThat(capturedUser.getName()).isEqualTo(testName);
    }

    @Test
    @DisplayName("upsertUser - Should update existing user when user exists")
    void upsertUser_WhenUserExists_ShouldUpdateUser() {
        // Arrange
        User existingUser = new User();
        existingUser.setId(testUserId);
        existingUser.setEmail("old@example.com");
        existingUser.setName("Old Name");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        String newEmail = "new@example.com";
        String newName = "New Name";

        // Act
        User result = userService.upsertUser(testUserId, newEmail, newName);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(newEmail);
        assertThat(result.getName()).isEqualTo(newName);

        verify(userRepository, times(1)).findById(testUserId);
        verify(userRepository, times(1)).save(existingUser);
    }

    @Test
    @DisplayName("upsertUser - Should not update name when new name is null")
    void upsertUser_WhenNameIsNull_ShouldNotUpdateName() {
        // Arrange
        String originalName = "Original Name";
        User existingUser = new User();
        existingUser.setId(testUserId);
        existingUser.setEmail("old@example.com");
        existingUser.setName(originalName);

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Act
        User result = userService.upsertUser(testUserId, testEmail, null);

        // Assert
        assertThat(result.getEmail()).isEqualTo(testEmail);
        assertThat(result.getName()).isEqualTo(originalName); // Name unchanged

        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("upsertUser - Should not update name when new name is empty string")
    void upsertUser_WhenNameIsEmpty_ShouldNotUpdateName() {
        // Arrange
        String originalName = "Original Name";
        User existingUser = new User();
        existingUser.setId(testUserId);
        existingUser.setEmail("old@example.com");
        existingUser.setName(originalName);

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Act
        User result = userService.upsertUser(testUserId, testEmail, "");

        // Assert
        assertThat(result.getEmail()).isEqualTo(testEmail);
        assertThat(result.getName()).isEqualTo(originalName); // Name unchanged

        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("upsertUser - Should update name when new name is valid and user exists")
    void upsertUser_WhenNameIsValidAndUserExists_ShouldUpdateName() {
        // Arrange
        User existingUser = new User();
        existingUser.setId(testUserId);
        existingUser.setEmail("old@example.com");
        existingUser.setName("Old Name");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        String newName = "New Name";

        // Act
        User result = userService.upsertUser(testUserId, testEmail, newName);

        // Assert
        assertThat(result.getName()).isEqualTo(newName);

        verify(userRepository).save(existingUser);
    }
}