package com.pctb.webapp.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminGroupStatsResponse {
    long totalGroups;
    long activeGroupsLast7Days;
    double averageMembersPerGroup;
}
