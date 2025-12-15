package com.jlgs.howmuchah.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSplitDto {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Amount owed is required")
    @PositiveOrZero(message = "Amount owed must be zero or positive")
    private BigDecimal amountOwed;
}
