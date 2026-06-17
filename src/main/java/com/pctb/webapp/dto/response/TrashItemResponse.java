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
public class TrashItemResponse {
    String type;
    String folderId;
    String documentId;
    String name;
    Long size;
    String parentFolderId;
    String documentFolderId;
    String fileExtension;
    String mimeType;
    String deletedAt;
}
