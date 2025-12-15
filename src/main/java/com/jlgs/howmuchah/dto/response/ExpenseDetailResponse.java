package com.jlgs.howmuchah.dto.response;

import com.jlgs.howmuchah.dto.UserSummary;
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
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseDetailResponse {

    private UUID id;
    private UUID groupId;
    private String description;
    private BigDecimal totalAmount;
    private String currency;
    private String category;
    private LocalDate expenseDate;

    // Full user info
    private UserSummary paidBy;

    // All splits
    private List<ExpenseSplitResponse> splits;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ExpenseDetailResponse from(Expense expense, List<ExpenseSplit> splits) {
        List<ExpenseSplitResponse> splitResponses = splits.stream()
                .map(ExpenseSplitResponse::from)
                .collect(Collectors.toList());

        return new ExpenseDetailResponse(
                expense.getId(),
                expense.getGroup().getId(),
                expense.getDescription(),
                expense.getTotalAmount(),
                expense.getCurrency(),
                expense.getCategory(),
                expense.getExpenseDate(),
                UserSummary.from(expense.getPaidBy()),
                splitResponses,
                expense.getCreatedAt(),
                expense.getUpdatedAt()
        );
    }
}