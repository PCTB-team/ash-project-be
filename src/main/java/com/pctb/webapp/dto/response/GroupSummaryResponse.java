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
public class GroupSummaryResponse {
    String groupId;

    String name;

    String description;

    String ownerId;

    String ownerName;

    String memberId;

    String role;

    Boolean canUpload;

    Boolean inviteEnabled;

    String inviteLink;

    Long memberCount;

    Long activeFileCount;

    String createdAt;

    String updatedAt;
}
