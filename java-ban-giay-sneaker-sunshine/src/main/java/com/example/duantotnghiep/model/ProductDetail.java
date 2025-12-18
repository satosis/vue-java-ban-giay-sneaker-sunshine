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
@Table(name = "product_details")
public class ProductDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "size_id")
    private Size size;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id")
    private Color color;

    @jakarta.validation.constraints.Size(max = 50)
    @Nationalized
    @Column(name = "product_detail_code", length = 50)
    private String productDetailCode;

    @Column(name = "sell_price", precision = 18, scale = 3)
    private BigDecimal sellPrice;

    @Column(name = "quantity")
    private Integer quantity;

    @jakarta.validation.constraints.Size(max = 250)
    @Nationalized
    @Column(name = "description", length = 250)
    private String description;

    @Column(name = "status")
    private Integer status;

    @Column(name = "created_date")
    private Date createdDate;

    @Column(name = "updated_date")
    private Date updatedDate;

    @jakarta.validation.constraints.Size(max = 50)
    @Column(name = "created_by", length = 50)
    private String createdBy;

    @jakarta.validation.constraints.Size(max = 50)
    @Column(name = "updated_by", length = 50)
    private String updatedBy;

}