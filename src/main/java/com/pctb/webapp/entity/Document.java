package com.pctb.webapp.entity;
import com.pctb.webapp.exception.UploadStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "document")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(nullable = false)
    String title;

    @Column(nullable = false)
    String fileName;

    @Column(nullable = false)
    String fileExtension;

    @Column(nullable = false)
    String mimeType;

    @Column(nullable = false)
    Long fileSize;

    @Column(nullable = false)
    String storageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    UploadStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User owner;

    @Column(nullable = false)
    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    @Builder.Default
    Boolean deleted = false;

    LocalDateTime deletedAt;
}
