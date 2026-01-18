package com.jlgs.howmuchah.controller;

import com.jlgs.howmuchah.dto.request.ClaimLinkRequest;
import com.jlgs.howmuchah.dto.response.InvitationLinkResponse;
import com.jlgs.howmuchah.dto.response.InvitationResponse;
import com.jlgs.howmuchah.dto.response.ValidateLinkResponse;
import com.jlgs.howmuchah.service.InvitationLinkService;
import com.jlgs.howmuchah.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class InvitationLinkController {

    private final JwtUtil jwtUtil;
    private final InvitationLinkService invitationLinkService;

    @GetMapping("/groups/{groupId}/invitation-links/current")
    public ResponseEntity<InvitationLinkResponse> getCurrentLink(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId) {

        UUID userId = jwtUtil.extractUserId(jwt);
        String userEmail = jwtUtil.extractEmail(jwt);
        log.info("User {} fetching current invitation link for group {}",
                Encode.forJava(userEmail), Encode.forJava(String.valueOf(groupId)));

        InvitationLinkResponse response = invitationLinkService.getCurrentOrGenerateLink(
                userId, groupId, userId
        );

        log.info("Returned invitation link {} for group {} to user {}",
                Encode.forJava(String.valueOf(response.getId())),
                Encode.forJava(String.valueOf(groupId)),
                Encode.forJava(userEmail));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/groups/{groupId}/invitation-links/regenerate")
    public ResponseEntity<InvitationLinkResponse> regenerateLink(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId) {

        UUID userId = jwtUtil.extractUserId(jwt);
        String userEmail = jwtUtil.extractEmail(jwt);
        log.info("User {} regenerating invitation link for group {}",
                Encode.forJava(userEmail), Encode.forJava(String.valueOf(groupId)));

        InvitationLinkResponse response = invitationLinkService.regenerateLink(
                userId, groupId, userId
        );

        log.info("Generated new invitation link {} for group {} by user {}",
                Encode.forJava(String.valueOf(response.getId())),
                Encode.forJava(String.valueOf(groupId)),
                Encode.forJava(userEmail));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/invitation-links/{linkId}/validate")
    public ResponseEntity<ValidateLinkResponse> validateLink(
            @PathVariable UUID linkId,
            @RequestParam String token) {

        log.info("Validating invitation link {}", Encode.forJava(String.valueOf(linkId)));

        ValidateLinkResponse response = invitationLinkService.validateLink(linkId, token);

        log.info("Invitation link {} validation result: {}",
                Encode.forJava(String.valueOf(linkId)),
                response.getValid() ? "valid" : "invalid");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/invitation-links/claim")
    public ResponseEntity<InvitationResponse> claimLink(
            @Valid @RequestBody ClaimLinkRequest request) {

        log.info("Claiming invitation link {} for email {}",
                Encode.forJava(String.valueOf(request.getLinkId())),
                Encode.forJava(request.getEmail()));

        InvitationResponse response = invitationLinkService.claimLink(request);

        log.info("Successfully created invitation {} via link {} for email {}",
                Encode.forJava(String.valueOf(response.getId())),
                Encode.forJava(String.valueOf(request.getLinkId())),
                Encode.forJava(request.getEmail()));

        return ResponseEntity.ok(response);
    }
}