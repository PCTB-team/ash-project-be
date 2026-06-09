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

/**
 * Luu thong tin chinh cua private group.
 * groupId chi dung noi bo, inviteToken moi dung de share link moi.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "study_group")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudyGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(nullable = false, length = 100)
    String name;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(nullable = false)
    String passwordHash;

    @Column(unique = true, nullable = false)
    String inviteToken;

    // Legacy column kept so existing MySQL tables with join_code NOT NULL can still insert.
    @Column(name = "join_code", unique = true, nullable = false, length = 20)
    String joinCode;

    // Legacy column from the old group flow. New logic always creates private groups.
    @Column(nullable = false, length = 20)
    String visibility;

    // Legacy password column from the old schema. Keep it synced with passwordHash.
    @Column(length = 255)
    String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    User owner;

    @Builder.Default
    @Column(nullable = false)
    Boolean inviteEnabled = true;

    @Column(nullable = false)
    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}
