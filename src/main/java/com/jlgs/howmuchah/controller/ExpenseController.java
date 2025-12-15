package com.jlgs.howmuchah.controller;

import com.jlgs.howmuchah.dto.request.ExpenseCreationRequest;
import com.jlgs.howmuchah.dto.request.ExpenseUpdateRequest;
import com.jlgs.howmuchah.dto.response.ExpenseDetailResponse;
import com.jlgs.howmuchah.dto.response.ExpenseResponse;
import com.jlgs.howmuchah.service.ExpenseService;
import com.jlgs.howmuchah.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Slf4j
public class ExpenseController {

    private final JwtUtil jwtUtil;
    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ExpenseDetailResponse> createExpense(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ExpenseCreationRequest request) {

        UUID userId = jwtUtil.extractUserId(jwt);
        log.info("User {} creating expense for group: {}", Encode.forJava(jwtUtil.extractEmail(jwt)), request.getGroupId());

        ExpenseDetailResponse response = expenseService.createExpense(userId, request);
        log.info("User {} successfully created expense for group: {}",
                Encode.forJava(jwtUtil.extractEmail(jwt)), request.getGroupId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{expenseId}")
    public ResponseEntity<ExpenseDetailResponse> updateExpense(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID expenseId,
            @Valid @RequestBody ExpenseUpdateRequest request) {

        UUID userId = jwtUtil.extractUserId(jwt);
        log.info("User {} updating expense: {}", Encode.forJava(jwtUtil.extractEmail(jwt)), expenseId);

        ExpenseDetailResponse response = expenseService.updateExpense(userId, expenseId, request);
        log.info("User {} successfully updated expense: {}", Encode.forJava(jwtUtil.extractEmail(jwt)), expenseId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ExpenseResponse>> getExpensesByGroup(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "expenseDate,desc") String[] sort) {

        UUID userId = jwtUtil.extractUserId(jwt);
        log.info("User {} fetching expenses for group: {}", Encode.forJava(jwtUtil.extractEmail(jwt)), groupId);

        // Parse sort parameters
        Sort.Direction direction = sort.length > 1 && sort[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        String sortBy = sort.length > 0 ? sort[0] : "expenseDate";

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<ExpenseResponse> expenses = expenseService.getExpensesByGroup(userId, groupId, pageable);

        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ExpenseDetailResponse> getExpenseDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID expenseId) {

        UUID userId = jwtUtil.extractUserId(jwt);
        log.info("User {} fetching expense detail: {}", Encode.forJava(jwtUtil.extractEmail(jwt)), expenseId);

        ExpenseDetailResponse response = expenseService.getExpenseDetail(userId, expenseId);
        log.info("User {} successfully fetched details for expense: {}",
                Encode.forJava(jwtUtil.extractEmail(jwt)), expenseId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID expenseId) {

        UUID userId = jwtUtil.extractUserId(jwt);
        log.info("User {} deleting expense: {}", Encode.forJava(jwtUtil.extractEmail(jwt)), expenseId);

        expenseService.deleteExpense(userId, expenseId);
        log.info("User {} successfully deleted expense: {}", Encode.forJava(jwtUtil.extractEmail(jwt)), expenseId);

        return ResponseEntity.noContent().build();
    }
}