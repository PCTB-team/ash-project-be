package com.pctb.webapp.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminTransactionResponse {
    String transactionId; // Mã transaction trong DB để FE quản lý
    Long orderCode;       // Mã đơn hàng của PayOS hiển thị lên bảng
    String username;      // Tên tài khoản người mua
    String email;         // Email người mua
    String planName;      // Tên gói cước (GO, PLUS, PRO)
    long amount;          // Số tiền nạp (VND)
    String status;        // Trạng thái (SUCCESS, PENDING, FAILED)
    LocalDateTime createdAt; // Ngày thực hiện giao dịch
}