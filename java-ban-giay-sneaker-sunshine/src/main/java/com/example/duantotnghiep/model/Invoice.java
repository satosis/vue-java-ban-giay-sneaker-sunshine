package com.example.duantotnghiep.model;

import com.example.duantotnghiep.state.TrangThaiChiTiet;
import com.example.duantotnghiep.state.TrangThaiChiTietConverter;
import com.example.duantotnghiep.state.TrangThaiTong;
import com.example.duantotnghiep.state.TrangThaiTongConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "invoice")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "invoice_code", length = 100, unique = true)
    private String invoiceCode;

    @Size(max = 50)
    @Column(name = "app_trans_id", length = 50, unique = true)
    private String appTransId;

    @Column(name = "shipping_fee")
    private BigDecimal shippingFee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "email")
    private String email;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "phone")
    private String phone;

    @Column(name = "final_amount")
    private BigDecimal finalAmount;

    @Size(max = 250)
    @Nationalized
    @Column(name = "description", length = 250)
    private String description;

    @Column(name = "order_type")
    private Integer orderType;

    @Convert(converter = TrangThaiTongConverter.class)
    @Column(name = "status", nullable = false)
    private TrangThaiTong status;

    @Column(name = "is_paid")
    private Boolean isPaid;

    @Convert(converter = TrangThaiChiTietConverter.class)
    @Column(name = "status_detail", nullable = false)
    private TrangThaiChiTiet statusDetail;

    @Column(name = "delivered_at")
    private Date deliveredAt;

    @Column(name = "request")
    private Integer request;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "phone_sender")
    private String phoneSender;

    @Column(name = "created_date")
    private Date createdDate;

    @Column(name = "updated_date")
    private Date updatedDate;

    @Size(max = 50)
    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Size(max = 50)
    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InvoiceDetail> invoiceDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

}