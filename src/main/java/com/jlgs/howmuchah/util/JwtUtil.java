package com.jlgs.howmuchah.util;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtil {

    public UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    public String extractEmail(Jwt jwt) {
        return jwt.getClaimAsString("email");
    }

    public String extractName(Jwt jwt) {
        // Try to get name from user_metadata
        Map<String, Object> userMetadata = jwt.getClaim("user_metadata");

        if (userMetadata != null) {
            Object fullName = userMetadata.get("full_name");
            if (fullName != null) {
                return fullName.toString();
            }

            Object name = userMetadata.get("name");
            if (name != null) {
                return name.toString();
            }
        }

        // Fallback to email if no name found
        return extractEmail(jwt);
    }
}