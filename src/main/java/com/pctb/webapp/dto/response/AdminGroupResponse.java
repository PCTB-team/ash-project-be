package com.pctb.webapp.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminGroupResponse {
    String id;
    String name;
    String leaderName;
    String leaderEmail;
    long memberCount;
    long fileCount;
    String status;
    LocalDateTime createdAt;
    List<GroupMemberResponse> members;
}
