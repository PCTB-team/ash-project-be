package com.pctb.webapp.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminDocumentResponse {
    String id;
    String fileName;
    String fileExtension;
    Long fileSize;
    String ownerUsername;
    String ownerEmail;
    @JsonIgnore
    boolean deleted;
    LocalDateTime createdAt;
}
