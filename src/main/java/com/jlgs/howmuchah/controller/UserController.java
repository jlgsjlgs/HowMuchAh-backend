package com.jlgs.howmuchah.controller;

import com.jlgs.howmuchah.dto.response.UserResponse;
import com.jlgs.howmuchah.entity.User;
import com.jlgs.howmuchah.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return userService.getUserById(id)
                .map(UserResponse::fromUser)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}