package com.pctb.webapp.dto.request;

import lombok.Data;

@Data
public class AdminUpdateGroupStatusRequest {
    String status;
    Boolean active;
}
