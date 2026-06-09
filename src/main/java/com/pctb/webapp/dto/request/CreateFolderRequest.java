package com.pctb.webapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateFolderRequest {
    @NotBlank(message = "FOLDER_NAME_REQUIRED")
    @Size(max = 100, message = "FOLDER_NAME_INVALID")
    String name;

    String parentFolderId;
}