package com.jlgs.howmuchah.dto.response;

import com.jlgs.howmuchah.dto.UserSummary;
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
}
