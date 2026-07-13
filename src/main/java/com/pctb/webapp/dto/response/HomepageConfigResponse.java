package com.pctb.webapp.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HomepageConfigResponse {
    String heroTitle;
    String heroSubtitle;
    String primaryColor;
    String videoBackgroundUrl;
    List<String> activeFeatures;
}
