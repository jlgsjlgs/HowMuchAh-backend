package com.jlgs.howmuchah.dto.response;


import com.jlgs.howmuchah.dto.UserSummary;
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
}
