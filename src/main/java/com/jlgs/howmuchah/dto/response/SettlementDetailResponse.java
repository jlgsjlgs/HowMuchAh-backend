package com.jlgs.howmuchah.dto.response;

import com.jlgs.howmuchah.entity.SettlementGroup;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDetailResponse {

    private UUID id;
    private LocalDateTime settledAt;
    private List<SettlementTransaction> transactions;

    public static SettlementDetailResponse from(SettlementGroup settlementGroup) {
        List<SettlementTransaction> transactions = settlementGroup.getSettlements().stream()
                .map(SettlementTransaction::from)
                .collect(Collectors.toList());

        return new SettlementDetailResponse(
                settlementGroup.getId(),
                settlementGroup.getSettledAt(),
                transactions
        );
    }
}