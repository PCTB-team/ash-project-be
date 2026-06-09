package com.pctb.webapp.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FolderResponse {
    String folderId;

    String name;

    String parentFolderId;

    Long size;

    Boolean deleted;

    String createdAt;

    String updatedAt;
}