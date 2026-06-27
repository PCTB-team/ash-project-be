package com.pctb.webapp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_log")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class SystemLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String actor;      // Tên admin
    @Transient
    String actorType;  // Ví dụ: "ADMIN"
    String action;     // Ví dụ: "LOCK_USER", "DELETE_DOCUMENT"
    String targetId;   // ID của user/document bị tác động
    @Column(columnDefinition = "TEXT")
    String details;    // Nội dung mô tả chi tiết
    LocalDateTime createdAt;

}