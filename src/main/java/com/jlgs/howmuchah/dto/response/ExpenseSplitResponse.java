package com.jlgs.howmuchah.dto.response;

import com.jlgs.howmuchah.dto.UserSummary;
import com.jlgs.howmuchah.entity.ExpenseSplit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSplitResponse {

    private UUID id;
    private UserSummary user;
    private BigDecimal amountOwed;
    private boolean isSettled;

    public static ExpenseSplitResponse from(ExpenseSplit split) {
        return new ExpenseSplitResponse(
                split.getId(),
                UserSummary.from(split.getUser()),
                split.getAmountOwed(),
                split.isSettled()
        );
    }
}