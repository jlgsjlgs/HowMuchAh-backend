package com.jlgs.howmuchah.controller;

import com.jlgs.howmuchah.config.TestSecurityConfig;
import com.jlgs.howmuchah.entity.User;
import com.jlgs.howmuchah.service.UserService;
import com.jlgs.howmuchah.util.JwtUtil;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserService userService;

    private UUID userId;
    private String email;
    private String name;
    private User testUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        email = "test@example.com";
        name = "Test User";

        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail(email);
        testUser.setName(name);
    }

    @Test
    @DisplayName("POST /api/auth/verify - Should verify and sync user successfully")
    void verifyAndSyncUser_WhenValidJwt_ShouldReturn200AndUserResponse() throws Exception {
        // Arrange
        when(jwtUtil.extractUserId(any(Jwt.class))).thenReturn(userId);
        when(jwtUtil.extractEmail(any(Jwt.class))).thenReturn(email);
        when(jwtUtil.extractName(any(Jwt.class))).thenReturn(name);
        when(userService.upsertUser(userId, email, name)).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(post("/api/auth/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value(name));

        // Verify
        verify(jwtUtil, times(1)).extractUserId(any(Jwt.class));
        verify(jwtUtil, times(1)).extractEmail(any(Jwt.class));
        verify(jwtUtil, times(1)).extractName(any(Jwt.class));
        verify(userService, times(1)).upsertUser(userId, email, name);
    }

    @Test
    @DisplayName("POST /api/auth/verify - Should handle user with null name")
    void verifyAndSyncUser_WhenNameIsNull_ShouldReturn200() throws Exception {
        // Arrange
        testUser.setName(null);

        when(jwtUtil.extractUserId(any(Jwt.class))).thenReturn(userId);
        when(jwtUtil.extractEmail(any(Jwt.class))).thenReturn(email);
        when(jwtUtil.extractName(any(Jwt.class))).thenReturn(null);
        when(userService.upsertUser(userId, email, null)).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(post("/api/auth/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value(email));

        verify(userService, times(1)).upsertUser(userId, email, null);
    }

    @Test
    @DisplayName("POST /api/auth/verify - Should return 500 when service throws exception")
    void verifyAndSyncUser_WhenServiceThrowsException_ShouldReturn500() throws Exception {
        // Arrange
        when(jwtUtil.extractUserId(any(Jwt.class))).thenReturn(userId);
        when(jwtUtil.extractEmail(any(Jwt.class))).thenReturn(email);
        when(jwtUtil.extractName(any(Jwt.class))).thenReturn(name);
        when(userService.upsertUser(userId, email, name))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(userService, times(1)).upsertUser(userId, email, name);
    }

    @Test
    @DisplayName("POST /api/auth/verify - Should extract JWT claims correctly")
    void verifyAndSyncUser_ShouldExtractJwtClaimsInCorrectOrder() throws Exception {
        // Arrange
        when(jwtUtil.extractUserId(any(Jwt.class))).thenReturn(userId);
        when(jwtUtil.extractEmail(any(Jwt.class))).thenReturn(email);
        when(jwtUtil.extractName(any(Jwt.class))).thenReturn(name);
        when(userService.upsertUser(userId, email, name)).thenReturn(testUser);

        // Act
        mockMvc.perform(post("/api/auth/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestSecurityConfig.TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Assert
        verify(jwtUtil).extractUserId(any(Jwt.class));
        verify(jwtUtil).extractEmail(any(Jwt.class));
        verify(jwtUtil).extractName(any(Jwt.class));
        verify(userService).upsertUser(eq(userId), eq(email), eq(name));
    }
}