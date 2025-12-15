package com.jlgs.howmuchah.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {

    private UUID id;
    private String description;
    private BigDecimal totalAmount;
    private String currency;
    private String category;
    private LocalDate expenseDate;

    // Simplified user info
    private UUID paidByUserId;
    private String paidByName;

    // Summary info
    private int splitCount;
    private int settledCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
