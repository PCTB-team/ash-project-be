package com.pctb.webapp.dto.request;

import lombok.Data;

@Data
public class PayOSWebhookRequest {
    private Long orderCode;
    private String code;
    private String desc;
    private String signature;
}