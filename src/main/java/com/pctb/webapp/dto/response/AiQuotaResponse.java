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
public class AiQuotaResponse {
    Long requestUsedToday;
    Long requestLimitPerDay;
    Long requestRemainingToday;
    Double requestUsagePercent;

    Long tokenUsedToday;
    Long tokenLimitPerDay;
    Long tokenRemainingToday;
    Double tokenUsagePercent;

    Long systemRequestUsedToday;
    Long systemRequestLimitPerDay;
    Long systemRequestRemainingToday;
    Double systemRequestUsagePercent;

    Long systemTokenUsedToday;
    Long systemTokenLimitPerDay;
    Long systemTokenRemainingToday;
    Double systemTokenUsagePercent;

    Boolean canChat;
}
