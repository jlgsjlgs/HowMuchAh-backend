package com.jlgs.howmuchah.controller;

import com.jlgs.howmuchah.dto.request.GroupCreationRequest;
import com.jlgs.howmuchah.dto.request.GroupUpdateRequest;
import com.jlgs.howmuchah.dto.request.InvitationRequest;
import com.jlgs.howmuchah.dto.response.GroupResponse;
import com.jlgs.howmuchah.dto.response.InvitationResponse;
import com.jlgs.howmuchah.entity.Group;
import com.jlgs.howmuchah.entity.Invitation;
import com.jlgs.howmuchah.service.GroupService;
import com.jlgs.howmuchah.service.InvitationService;
import com.jlgs.howmuchah.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.owasp.encoder.Encode;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Slf4j
public class GroupController {

    private final JwtUtil jwtUtil;
    private final GroupService groupService;
    private final InvitationService invitationService;

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody GroupCreationRequest request) {

        UUID userId = jwtUtil.extractUserId(jwt);
        log.info("User {} creating group", Encode.forJava(jwtUtil.extractEmail(jwt)));

        Group createdGroup = groupService.createGroup(userId, request);
        log.info("User {} successfully created group {} with groupId {}", userId, Encode.forJava(createdGroup.getName()), createdGroup.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GroupResponse.fromGroup(createdGroup));
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getAllGroups(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = jwtUtil.extractUserId(jwt);
        log.info("Getting all groups for {}", Encode.forJava(jwtUtil.extractEmail(jwt)));

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
        log.info("User {} deleting group {}", Encode.forJava(jwtUtil.extractEmail(jwt)), Encode.forJava(String.valueOf(groupId)));

        groupService.deleteGroup(groupId, userId);
        log.info("Successfully deleted group {}", Encode.forJava(String.valueOf(groupId)));

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{groupId}")
    public ResponseEntity<GroupResponse> updateGroup(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId,
            @RequestBody GroupUpdateRequest request) {

        UUID userId = jwtUtil.extractUserId(jwt);
        log.info("User {} updating group {}", Encode.forJava(jwtUtil.extractEmail(jwt)), Encode.forJava(String.valueOf(groupId)));

        Group updatedGroup = groupService.updateGroup(groupId, userId, request);
        log.info("Successfully updated group {} details", Encode.forJava(String.valueOf(groupId)));

        return ResponseEntity.ok(GroupResponse.fromGroup(updatedGroup));
    }

    @PostMapping("/{groupId}/invitations")
    public ResponseEntity<InvitationResponse> sendInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId,
            @RequestBody InvitationRequest request) {

        UUID userId = jwtUtil.extractUserId(jwt);
        log.info("User {} sending invitation to {} for group {}",
                Encode.forJava(jwtUtil.extractEmail(jwt)), Encode.forJava(request.getInvitedEmail()), Encode.forJava(String.valueOf(groupId)));

        Invitation invitation = invitationService.sendInvitation(groupId, userId, request);
        log.info("Successfully invited {} to group {}", Encode.forJava(request.getInvitedEmail()), Encode.forJava(String.valueOf(groupId)));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InvitationResponse.fromInvitation(invitation));
    }

    @GetMapping("/{groupId}/invitations")
    public ResponseEntity<List<InvitationResponse>> getAllInvitations(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId) {

        UUID userId = jwtUtil.extractUserId(jwt);
        log.info("User {} getting all invitations for group {}",
                Encode.forJava(jwtUtil.extractEmail(jwt)), Encode.forJava(String.valueOf(groupId)));

        List<Invitation> invitations = invitationService.getAllInvitationsForGroup(groupId, userId);
        log.info("Found {} invitations for group {}", invitations.size(), Encode.forJava(String.valueOf(groupId)));

        List<InvitationResponse> invitationResponses = invitations.stream()
                .map(InvitationResponse::fromInvitation)
                .collect(Collectors.toList());

        return ResponseEntity.ok(invitationResponses);
    }

    @PostMapping("/{groupId}/invitations/{invitationId}")
    public ResponseEntity<Void> revokeInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId,
            @PathVariable UUID invitationId) {

        UUID userId = jwtUtil.extractUserId(jwt);
        log.info("User {} revoking invitation {} for group {}",
                Encode.forJava(jwtUtil.extractEmail(jwt)), Encode.forJava(String.valueOf(invitationId)), Encode.forJava(String.valueOf(groupId)));

        invitationService.revokeInvitation(groupId, invitationId, userId);
        log.info("Successfully revoked invitation {} for group {}",
                Encode.forJava(Encode.forJava(String.valueOf(invitationId))), Encode.forJava(String.valueOf(groupId)));

        return ResponseEntity.noContent().build();
    }
}
