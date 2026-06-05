package com.pctb.webapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateDocumentRequest {
    @NotBlank(message = "DOCUMENT_FILE_NAME_REQUIRED")
    String fileName;
}
