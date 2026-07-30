package com.pctb.webapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultResponse {
    private String transactionId;
    private Long orderCode;
    private String status;
    private String displayStatus;
    private String title;
    private String message;
    private boolean planGranted;
}
