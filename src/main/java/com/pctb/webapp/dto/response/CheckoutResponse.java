package com.pctb.webapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponse {

    private String transactionId;
    private String checkoutUrl;
    private Long amount;
    private Long orderCode;
}