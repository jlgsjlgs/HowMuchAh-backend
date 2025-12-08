package com.jlgs.howmuchah.controller;

import com.jlgs.howmuchah.dto.response.UserResponse;
import com.jlgs.howmuchah.entity.User;
import com.jlgs.howmuchah.util.JwtUtil;
import com.jlgs.howmuchah.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.owasp.encoder.Encode;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @PostMapping("/verify")
    public ResponseEntity<UserResponse> verifyAndSyncUser(
            @AuthenticationPrincipal Jwt jwt) {

        // Extract user info from JWT
        UUID userId = jwtUtil.extractUserId(jwt);
        String email = jwtUtil.extractEmail(jwt);
        String name = jwtUtil.extractName(jwt);

        // Upsert user to database
        User user = userService.upsertUser(userId, email, name);

        log.info("User successfully synced - {}", Encode.forJava(email));

        return ResponseEntity.ok(UserResponse.fromUser(user));
    }
}