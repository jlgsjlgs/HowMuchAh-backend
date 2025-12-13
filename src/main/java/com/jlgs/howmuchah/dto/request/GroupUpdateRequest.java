package com.jlgs.howmuchah.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupUpdateRequest {

    @Size(min = 1, max = 50, message = "Group name must be between 1 and 50 characters")
    private String name;

    @Size(max = 150, message = "Description must not exceed 150 characters")
    private String description;
}
