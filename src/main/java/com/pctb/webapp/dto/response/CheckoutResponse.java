package com.pctb.webapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponse {

    private String transactionId;
    private String checkoutUrl;
    private Long amount;
    private Long orderCode;
    private LocalDateTime expiredAt;
}
