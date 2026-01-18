package com.jlgs.howmuchah.dto.response;

import com.jlgs.howmuchah.entity.InvitationLink;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkDetailsResponse {
    private UUID linkId;
    private UUID groupId;
    private String groupName;
    private String createdByName;
    private Integer maxUses;
    private Integer currentUses;
    private LocalDateTime expiresAt;

    public static LinkDetailsResponse fromInvitationLink(InvitationLink link) {
        return new LinkDetailsResponse(
                link.getId(),
                link.getGroup().getId(),
                link.getGroup().getName(),
                link.getCreatedBy().getName(),
                link.getMaxUses(),
                link.getCurrentUses(),
                link.getExpiresAt()
        );
    }
}