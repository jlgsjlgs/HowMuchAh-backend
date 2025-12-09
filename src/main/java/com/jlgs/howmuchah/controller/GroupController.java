package com.jlgs.howmuchah.controller;

import com.jlgs.howmuchah.dto.request.GroupCreationRequest;
import com.jlgs.howmuchah.dto.request.GroupUpdateRequest;
import com.jlgs.howmuchah.dto.response.GroupResponse;
import com.jlgs.howmuchah.entity.Group;
import com.jlgs.howmuchah.service.GroupService;
import com.jlgs.howmuchah.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final JwtUtil jwtUtil;
    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody GroupCreationRequest request) {

        UUID userId = jwtUtil.extractUserId(jwt);
        Group createdGroup = groupService.createGroup(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GroupResponse.fromGroup(createdGroup));
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getAllGroups(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = jwtUtil.extractUserId(jwt);
        List<Group> groups = groupService.getAllGroupsForUser(userId);

        List<GroupResponse> groupResponses = groups.stream()
                .map(GroupResponse::fromGroup)
                .collect(Collectors.toList());

        return ResponseEntity.ok(groupResponses);
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId) {

        UUID userId = jwtUtil.extractUserId(jwt);
        groupService.deleteGroup(groupId, userId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{groupId}")
    public ResponseEntity<GroupResponse> updateGroup(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId,
            @RequestBody GroupUpdateRequest request) {

        UUID userId = jwtUtil.extractUserId(jwt);
        Group updatedGroup = groupService.updateGroup(groupId, userId, request);

        return ResponseEntity.ok(GroupResponse.fromGroup(updatedGroup));
    }
}
