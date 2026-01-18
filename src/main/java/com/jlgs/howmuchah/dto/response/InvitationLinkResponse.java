package com.jlgs.howmuchah.dto.response;

import com.jlgs.howmuchah.entity.InvitationLink;
import com.jlgs.howmuchah.util.InvitationLinkMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationLinkResponse {
    private UUID id;
    private String token;
    private String link;
    private Integer maxUses;
    private Integer currentUses;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public static InvitationLinkResponse fromInvitationLink(InvitationLink invitationLink, String baseUrl) {
        return InvitationLinkResponse.builder()
                .id(invitationLink.getId())
                .token(invitationLink.getToken())
                .link(InvitationLinkMapper.buildLinkUrl(
                        invitationLink.getId(),
                        invitationLink.getToken(),
                        baseUrl
                ))
                .maxUses(invitationLink.getMaxUses())
                .currentUses(invitationLink.getCurrentUses())
                .expiresAt(invitationLink.getExpiresAt())
                .createdAt(invitationLink.getCreatedAt())
                .build();
    }
}
