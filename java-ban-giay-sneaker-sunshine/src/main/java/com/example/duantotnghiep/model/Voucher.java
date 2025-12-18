package com.example.duantotnghiep.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "voucher")
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Size(max = 150)
    @NotNull
    @Nationalized
    @Column(name = "voucher_code", nullable = false, length = 150)
    private String voucherCode;

    @Size(max = 250)
    @Nationalized
    @Column(name = "voucher_name", length = 250)
    private String voucherName;

    @Column(name = "discount_percentage")
    private BigDecimal discountPercentage;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "discount_amount", nullable = false)
    private Integer discountAmount;

    @NotNull
    @Column(name = "min_order_value", nullable = false)
    private BigDecimal minOrderValue;

    @Column(name = "max_discount_value")
    private BigDecimal maxDiscountValue;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "status")
    private Integer status;

    @Size(max = 250)
    @Nationalized
    @Column(name = "description", length = 250)
    private String description;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Size(max = 50)
    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Size(max = 50)
    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "order_type")
    private Integer orderType;

    @Column(name = "voucher_type")
    private Integer voucherType;

    @OneToMany(mappedBy = "voucher")
    private Set<Invoice> invoices = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "min_order_to_receive")
    private BigDecimal minOrderToReceive;

    @OneToMany(mappedBy = "voucher")
    private Set<VoucherHistory> voucherHistories = new LinkedHashSet<>();

}