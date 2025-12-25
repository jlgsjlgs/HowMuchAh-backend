package com.jlgs.howmuchah.controller;

import com.jlgs.howmuchah.dto.response.SettlementDetailResponse;
import com.jlgs.howmuchah.dto.response.SettlementSummaryResponse;
import com.jlgs.howmuchah.service.SettlementService;
import com.jlgs.howmuchah.util.JwtUtil;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
@Slf4j
public class SettlementController {

    private final JwtUtil jwtUtil;
    private final SettlementService settlementService;

    @GetMapping("/{groupId}/history")
    public ResponseEntity<List<SettlementSummaryResponse>> getSettlementHistory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId) {

        UUID userId = jwtUtil.extractUserId(jwt);
        log.info("User {} getting settlement history for group {}",
                Encode.forJava(jwtUtil.extractEmail(jwt)), Encode.forJava(String.valueOf(groupId)));

        List<SettlementSummaryResponse> history = settlementService.getSettlementHistory(userId, groupId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{settlementGroupId}")
    public ResponseEntity<SettlementDetailResponse> getSettlementDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID settlementGroupId) {

        UUID userId = jwtUtil.extractUserId(jwt);
        log.info("User {} getting settlement details for settlement {}",
                Encode.forJava(jwtUtil.extractEmail(jwt)), Encode.forJava(String.valueOf(settlementGroupId)));

        SettlementDetailResponse detail = settlementService.getSettlementDetail(userId, settlementGroupId);
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/{groupId}/settle")
    public ResponseEntity<SettlementDetailResponse> executeSettlement(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId) {

        UUID userId = jwtUtil.extractUserId(jwt);
        log.info("User {} settling expenses for group {}",
                Encode.forJava(jwtUtil.extractEmail(jwt)), Encode.forJava(String.valueOf(groupId)));

        SettlementDetailResponse result = settlementService.executeSettlement(userId, groupId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}