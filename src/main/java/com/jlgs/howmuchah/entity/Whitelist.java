package com.jlgs.howmuchah.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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

    @CreationTimestamp
    @Column(name = "whitelisted_at", nullable = false, updatable = false)
    private LocalDateTime whitelistedAt;
}
