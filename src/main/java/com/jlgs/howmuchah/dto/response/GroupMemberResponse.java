package com.jlgs.howmuchah.dto.response;

import com.jlgs.howmuchah.entity.GroupMember;
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
public class GroupMemberResponse {
    private UUID userId;
    private String userName;
    private String userEmail;
    private LocalDateTime joinedAt;
    private boolean isOwner;

    public static GroupMemberResponse fromGroupMember(GroupMember member) {
        return GroupMemberResponse.builder()
                .userId(member.getUser().getId())
                .userName(member.getUser().getName())
                .userEmail(member.getUser().getEmail())
                .joinedAt(member.getJoinedAt())
                .isOwner(member.getGroup().getOwner().getId().equals(member.getUser().getId()))
                .build();
    }
}