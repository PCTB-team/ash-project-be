package com.pctb.webapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CheckoutResponse {
    private String transactionId;
    private String checkoutUrl;
    private Integer amount;
    private Long orderCode;
}