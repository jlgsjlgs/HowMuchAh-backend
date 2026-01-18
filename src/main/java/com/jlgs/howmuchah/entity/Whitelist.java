package com.jlgs.howmuchah.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "whitelist", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Whitelist {

    @Id
    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "whitelisted_at")
    private LocalDateTime whitelistedAt;
}
