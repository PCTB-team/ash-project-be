package com.pctb.webapp.dto.request;

import lombok.Data;

@Data
public class AdminUpdatePlanRequest {
    String planName;
    Long quotaSize;
    Long price;
    Integer durationMonths;
}
