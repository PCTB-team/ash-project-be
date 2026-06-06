package com.pctb.webapp.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentResponse {
    String documentId;

    String fileName;

    String fileExtension;

    String mimeType;

    Long fileSize;

    String storageUrl;

    String status;

    String uploadedAt;

    String timeSinceUpload;

    Boolean deleted;

    String deletedAt;
}
