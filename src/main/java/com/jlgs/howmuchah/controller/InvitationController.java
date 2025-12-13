package com.jlgs.howmuchah.controller;

import com.jlgs.howmuchah.dto.response.InvitationResponse;
import com.jlgs.howmuchah.entity.Invitation;
import com.jlgs.howmuchah.service.GroupService;
import com.jlgs.howmuchah.service.InvitationService;
import com.jlgs.howmuchah.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
@Slf4j
public class InvitationController {

    private final JwtUtil jwtUtil;
    private final InvitationService invitationService;

    @GetMapping("/pending")
    public ResponseEntity<List<InvitationResponse>> getPendingInvitations(
            @AuthenticationPrincipal Jwt jwt) {

        String userEmail = jwtUtil.extractEmail(jwt);
        log.info("User {} fetching pending group invitations", Encode.forJava(userEmail));

        List<Invitation> invitations = invitationService.getPendingInvitationsForEmail(userEmail);
        log.info("Retrieved {} pending invitations for user {}", invitations.size() ,Encode.forJava(userEmail));

        List<InvitationResponse> invitationResponses = invitations.stream()
                .map(InvitationResponse::fromInvitation)
                .collect(Collectors.toList());

        return ResponseEntity.ok(invitationResponses);
    }

    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<InvitationResponse> acceptInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID invitationId) {

        UUID userId = jwtUtil.extractUserId(jwt);
        String userEmail = jwtUtil.extractEmail(jwt);
        log.info("User {} accepting group invitation {}", Encode.forJava(userEmail), Encode.forJava(String.valueOf(invitationId)));

        Invitation invitation = invitationService.acceptInvitation(invitationId, userId, userEmail);
        log.info("User {} added to group via invitation {}",
                Encode.forJava(userEmail), Encode.forJava(String.valueOf(invitationId)));

        return ResponseEntity.ok(InvitationResponse.fromInvitation(invitation));
    }

    @PostMapping("/{invitationId}/decline")
    public ResponseEntity<InvitationResponse> declineInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID invitationId) {

        String userEmail = jwtUtil.extractEmail(jwt);
        log.info("User {} declining group invitation {}", Encode.forJava(userEmail), Encode.forJava(String.valueOf(invitationId)));

        Invitation invitation = invitationService.declineInvitation(invitationId, userEmail);
        log.info("Invitation {} declined by user {}",
                Encode.forJava(String.valueOf(invitationId)), Encode.forJava(userEmail));

        return ResponseEntity.ok(InvitationResponse.fromInvitation(invitation));
    }
}
