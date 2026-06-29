package com.pctb.webapp.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminAiStatsResponse {
    long totalAiMessagesThisMonth;
    long topAiUserMessageCount;
    double knowledgeChatRatio;
    long totalSummarizedDocs;
    Map<String, Long> aiUsageTrendByDay;
}