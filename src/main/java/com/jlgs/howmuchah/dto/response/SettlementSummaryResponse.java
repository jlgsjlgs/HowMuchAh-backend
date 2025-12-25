package com.jlgs.howmuchah.dto.response;

import com.jlgs.howmuchah.entity.SettlementGroup;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementSummaryResponse {

    private UUID id;
    private LocalDateTime settledAt;
    private int transactionCount;

    public static SettlementSummaryResponse from(SettlementGroup settlementGroup) {
        return new SettlementSummaryResponse(
                settlementGroup.getId(),
                settlementGroup.getSettledAt(),
                settlementGroup.getSettlements().size()
        );
    }
}