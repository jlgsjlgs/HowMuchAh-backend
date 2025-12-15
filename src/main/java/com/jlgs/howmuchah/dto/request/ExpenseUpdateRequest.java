package com.jlgs.howmuchah.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseUpdateRequest {

    @NotBlank(message = "Description is required")
    @Size(max = 100, message = "Description must be less than 100 characters")
    private String description;

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private BigDecimal totalAmount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotNull(message = "Paid by user ID is required")
    private UUID paidByUserId;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    @NotEmpty(message = "At least one split is required")
    @Valid
    private List<ExpenseSplitDto> splits;
}
