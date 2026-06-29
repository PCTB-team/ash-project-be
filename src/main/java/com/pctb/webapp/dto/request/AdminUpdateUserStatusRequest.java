package com.pctb.webapp.dto.request;

import lombok.Data;

@Data
public class AdminUpdateUserStatusRequest {
    String status;
    Boolean active;
    String reason;
}
