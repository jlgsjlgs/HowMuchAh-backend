package com.jlgs.howmuchah.dto.response;

import com.jlgs.howmuchah.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private String name;

    // Convenience constructor from User entity
    public static UserResponse fromUser(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName()
        );
    }
}