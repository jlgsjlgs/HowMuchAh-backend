package com.jlgs.howmuchah.controller;

import com.jlgs.howmuchah.dto.response.SettlementDetailResponse;
import com.jlgs.howmuchah.dto.response.SettlementSummaryResponse;
import com.jlgs.howmuchah.service.SettlementService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping("/{groupId}/history")
    public ResponseEntity<List<SettlementSummaryResponse>> getSettlementHistory(
            @PathVariable @NotNull UUID groupId) {

        List<SettlementSummaryResponse> history = settlementService.getSettlementHistory(groupId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{settlementGroupId}")
    public ResponseEntity<SettlementDetailResponse> getSettlementDetail(
            @PathVariable @NotNull UUID settlementGroupId) {

        SettlementDetailResponse detail = settlementService.getSettlementDetail(settlementGroupId);
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/{groupId}/settle")
    public ResponseEntity<SettlementDetailResponse> executeSettlement(
            @PathVariable @NotNull UUID groupId) {

        SettlementDetailResponse result = settlementService.executeSettlement(groupId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}