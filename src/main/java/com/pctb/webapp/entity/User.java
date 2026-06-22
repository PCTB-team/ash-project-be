package com.pctb.webapp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;


import java.time.LocalDateTime;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "user")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Tạo id = 1 dãy ngẫu nhiên
    String id;
    @Column(unique = true,nullable = false,length = 50)
    String username;
    @Column(nullable = false)
    String password;
    @Column(nullable = false, length = 100)
    String fullname;

    @Column(unique = true, nullable = false, length = 100)
    String email;

    // Lưu URL public của avatar, file thật nằm trong local storage.
    @Column(name = "avatar_url", length = 255)
    String avatarUrl;

    // Trường học do user tự nhập trong trang profile.
    @Column(length = 100)
    String school;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_name")
    )
    Set<Role> roles;

    @Column(nullable = false)
    boolean verified = false; /// Dùng để theo dõi trạng thái xác thực của user.

    // === PHẦN BỔ SUNG ADMIN BUSINESS RULES ===
    @Column(nullable = false)
    boolean accountNonLocked = true; // true: Bình thường, false: Bị khóa

    @Column(name = "locked_at")
    LocalDateTime lockedAt;

    @Column(name = "locked_reason", length = 255)
    String lockedReason;

    @Column(name = "locked_by_admin")
    String lockedByAdmin; // Lưu username hoặc ID của Admin thực hiện

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // === CẤU TRÚC LẠI BỘ NHỚ VIP THEO BUSINESS MỚI ===
    @Column(name = "storage_quota", nullable = false)
    Long storageQuota = 524288000L; // Mặc định ban đầu 500MB (= 500 * 1024 * 1024 bytes)

    @Column(name = "storage_used", nullable = false)
    Long storageUsed = 0L;

    @Column(name = "storage_expired_at")
    LocalDateTime storageExpiredAt; // null tương ứng với dùng gói 500MB mặc định vĩnh viễn
}
