package com.pctb.webapp.dto.request;

import lombok.Data;

@Data
public class AdminUpdateSettingsRequest {
    String applicationName;
    Boolean maintenanceMode;
    Long defaultStorageLimit;
    Long maxFileSizeUpload;
    String allowedFileTypes;
    Integer otpExpiryMinutes;
    Integer sessionTimeoutMinutes;
    Integer maxLoginAttempts;
    Boolean emailNotificationEnabled;
    Boolean allowRegistration;
    Long defaultUserStorage;
}
