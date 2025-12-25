package com.jlgs.howmuchah.dto.response;

import com.jlgs.howmuchah.dto.UserSummary;
import com.jlgs.howmuchah.entity.Settlement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementTransaction {

    private UserSummary payer;
    private UserSummary payee;
    private BigDecimal amount;
    private String currency;

    public static SettlementTransaction from(Settlement settlement) {
        return new SettlementTransaction(
                UserSummary.from(settlement.getPayer()),
                UserSummary.from(settlement.getPayee()),
                settlement.getAmount(),
                settlement.getCurrency()
        );
    }
}