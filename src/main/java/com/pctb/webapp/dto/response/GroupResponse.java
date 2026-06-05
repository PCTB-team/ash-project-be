package com.pctb.webapp.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupResponse {
    String id;
    String name;
    String description;
    String joinCode;
    String visibility;
    String ownerId;
    LocalDateTime createdAt;
}