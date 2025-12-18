package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InvoiceTransactionResponse {
    private Long transactionId;
    private Long invoiceId;
    private String transactionCode;
    private String transactionType;
    private String paymentMethod;

    private Date paymentTime;
    private String transactionNote;
}
