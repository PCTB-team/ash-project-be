package com.pctb.webapp.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class AdminUpdatePlanRequest {
    String name;
    String planName;
    Long quotaSize;
    Long price;
    Integer durationMonths;
    String status;
    List<String> features;
}
