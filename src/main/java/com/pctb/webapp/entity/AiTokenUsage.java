package com.pctb.webapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ai_token_usage")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiTokenUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false, length = 100)
    String model;

    @Column(nullable = false, length = 50)
    String feature;

    @Column(name = "prompt_tokens", nullable = false)
    Integer promptTokens;

    @Column(name = "output_tokens", nullable = false)
    Integer outputTokens;

    @Column(name = "total_tokens", nullable = false)
    Integer totalTokens;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;
}
