package com.example.duantotnghiep.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "invoice_transaction")
public class InvoiceTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 100)
    @Column(name = "transaction_code", length = 100)
    private String transactionCode;

    @Size(max = 100)
    @Column(name = "trade_code", length = 100)
    private String tradeCode;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Size(max = 50)
    @Column(name = "transaction_type", length = 50)
    private String transactionType;

    @Column(name = "bank_name")
    private String bankName;

    @Size(max = 50)
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_status")
    private Integer paymentStatus;

    @Column(name = "payment_time")
    private Date paymentTime;

    @Column(name = "amount")
    private BigDecimal amount;

    @Size(max = 255)
    @Nationalized
    @Column(name = "note")
    private String note;

}