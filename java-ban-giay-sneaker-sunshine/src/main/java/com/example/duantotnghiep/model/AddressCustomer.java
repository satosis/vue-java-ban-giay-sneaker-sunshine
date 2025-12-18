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
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "address_customer")
public class AddressCustomer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Size(max = 50)
    @Nationalized
    @Column(name = "country", length = 50)
    private String country;

    @Size(max = 100)
    @Nationalized
    @Column(name = "province_code", length = 100)
    private String provinceCode;

    @Size(max = 100)
    @Nationalized
    @Column(name = "province_name", length = 100)
    private String provinceName;

    @Size(max = 100)
    @Nationalized
    @Column(name = "district_code", length = 100)
    private String districtCode;

    @Size(max = 100)
    @Nationalized
    @Column(name = "district_name", length = 100)
    private String districtName;

    @Size(max = 100)
    @Nationalized
    @Column(name = "ward_code", length = 100)
    private String wardCode;

    @Size(max = 100)
    @Nationalized
    @Column(name = "ward_name", length = 100)
    private String wardName;

    @Size(max = 250)
    @Nationalized
    @Column(name = "house_name", length = 250)
    private String houseName;

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

    @Column(name = "status")
    private Integer status;

    @Column(name = "default_address")
    private Boolean defaultAddress;

}