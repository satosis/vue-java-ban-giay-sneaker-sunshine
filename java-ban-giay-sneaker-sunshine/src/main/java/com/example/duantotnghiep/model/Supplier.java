package com.example.duantotnghiep.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "supplier")
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 50)
    @Column(name = "supplier_code", length = 50)
    private String supplierCode;

    @Size(max = 250)
    @NotNull
    @Nationalized
    @Column(name = "supplier_name", nullable = false, length = 250)
    private String supplierName;

    @Size(max = 50)
    @NotNull
    @Nationalized
    @Column(name = "country", nullable = false, length = 50)
    private String country;

    @Size(max = 100)
    @NotNull
    @Nationalized
    @Column(name = "province", nullable = false, length = 100)
    private String province;

    @Size(max = 100)
    @NotNull
    @Nationalized
    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @Size(max = 100)
    @NotNull
    @Nationalized
    @Column(name = "ward", nullable = false, length = 100)
    private String ward;

    @Size(max = 250)
    @NotNull
    @Nationalized
    @Column(name = "house_name", nullable = false, length = 250)
    private String houseName;

    @Column(name = "supplier_status")
    private Integer supplierStatus;

    @Column(name = "created_date")
    private Date createdDate;

    @Column(name = "updated_date")
    private Date updatedDate;

    @Size(max = 100)
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Size(max = 100)
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

}