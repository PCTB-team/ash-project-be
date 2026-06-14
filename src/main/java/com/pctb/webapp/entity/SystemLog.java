package com.pctb.webapp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 50)
    String actor; // Admin thực hiện hành động

    @Column(nullable = false, length = 100)
    String action; // Tên hành động (LOCK_USER, UNLOCK_USER, VIEW_LOGS,...)

    @Column(columnDefinition = "TEXT")
    String details; // Chi tiết: "Khóa user 'nguyenvana' vì lý do: Gửi prompt độc hại"

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;
}