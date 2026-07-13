package com.pctb.webapp.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class AdminUpdateHomepageConfigRequest {
    String heroTitle;
    String heroSubtitle;
    String primaryColor;
    String videoBackgroundUrl;
    List<String> activeFeatures;
}
