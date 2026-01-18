package com.jlgs.howmuchah.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimLinkRequest {

    @NotNull(message = "Link ID is required")
    private UUID linkId;

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}