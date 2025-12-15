package com.jlgs.howmuchah.dto;

import com.jlgs.howmuchah.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummary {
    private UUID id;
    private String name;
    private String email;

    public static UserSummary from(User user) {
        return new UserSummary(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}