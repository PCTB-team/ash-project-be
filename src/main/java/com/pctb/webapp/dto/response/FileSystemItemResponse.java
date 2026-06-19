package com.pctb.webapp.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
// Trả về response get item  bao gồm file và folder
public class FileSystemItemResponse {
    String id;

    String type;

    String name;

    String fileExtension;

    String mimeType;

    Long size;

    String parentFolderId;

    String storageUrl;

    String viewUrl;

    String downloadUrl;

    String status;

    String createdAt;

    String updatedAt;

    String timeSinceCreated;

    Boolean deleted;
}
