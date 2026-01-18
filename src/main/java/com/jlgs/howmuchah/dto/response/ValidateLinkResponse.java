package com.jlgs.howmuchah.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateLinkResponse {
    private Boolean valid;
    private LinkDetailsResponse linkDetails;  // Only populated if valid = true

    public static ValidateLinkResponse valid(LinkDetailsResponse details) {
        return ValidateLinkResponse.builder()
                .valid(true)
                .linkDetails(details)
                .build();
    }
}