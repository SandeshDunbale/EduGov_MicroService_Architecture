package com.project.edugov.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "blacklisted_tokens")
@Data
public class BlackListedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tokenId;

    @Column(nullable = false, length = 1000)
    private String token;
}