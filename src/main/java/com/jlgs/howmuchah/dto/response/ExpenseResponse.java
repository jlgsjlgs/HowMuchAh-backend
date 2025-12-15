package com.jlgs.howmuchah.dto.response;

import com.jlgs.howmuchah.entity.Expense;
import com.jlgs.howmuchah.entity.ExpenseSplit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private boolean isSettled;

    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getTotalAmount(),
                expense.getCurrency(),
                expense.getCategory(),
                expense.getExpenseDate(),
                expense.getPaidBy().getId(),
                expense.getPaidBy().getName(),
                expense.getCreatedAt(),
                expense.getUpdatedAt(),
                expense.isSettled()
        );
    }
}