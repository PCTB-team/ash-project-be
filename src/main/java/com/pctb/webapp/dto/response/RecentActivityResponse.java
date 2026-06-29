package com.pctb.webapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecentActivityResponse {

    private String actor;

    private String action;

    private String detail;

    private LocalDateTime createdAt;

}
