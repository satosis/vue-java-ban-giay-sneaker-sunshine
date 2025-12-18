package com.example.duantotnghiep.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "favorite_product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private Date createdAt;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "status")
    private Integer status;

    @Column(name = "rate")
    private Integer rate;

    @Column(name = "comment")
    private String comment;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;


}
