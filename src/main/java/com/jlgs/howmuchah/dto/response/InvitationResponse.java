package com.jlgs.howmuchah.dto.response;

import com.jlgs.howmuchah.entity.Invitation;
import com.jlgs.howmuchah.enums.InvitationStatus;
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
public class InvitationResponse {
    private UUID id;
    private UUID groupId;
    private String groupName;
    private String invitedEmail;
    private String invitedBy;
    private String invitedByEmail;
    private InvitationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InvitationResponse fromInvitation(Invitation invitation) {
        return InvitationResponse.builder()
                .id(invitation.getId())
                .groupId(invitation.getGroup().getId())
                .groupName(invitation.getGroup().getName())
                .invitedEmail(invitation.getInvitedEmail())
                .invitedBy(invitation.getInvitedBy().getName())
                .invitedByEmail(invitation.getInvitedBy().getEmail())
                .status(invitation.getStatus())
                .createdAt(invitation.getCreatedAt())
                .updatedAt(invitation.getUpdatedAt())
                .build();
    }
}
