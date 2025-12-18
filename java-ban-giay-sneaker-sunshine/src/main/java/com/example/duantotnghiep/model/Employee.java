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

import java.math.BigDecimal;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "employee")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 50)
    @NotNull
    @Column(name = "employee_code", nullable = false, length = 50)
    private String employeeCode;

    @Size(max = 250)
    @NotNull
    @Nationalized
    @Column(name = "employee_name", nullable = false, length = 250)
    private String employeeName;

    @NotNull
    @Column(name = "date_of_birth", nullable = false)
    private Date dateOfBirth;

    @NotNull
    @Column(name = "gender", nullable = false)
    private Integer gender;

    @Size(max = 50)
    @NotNull
    @Column(name = "phone", nullable = false, length = 50)
    private String phone;

    @Size(max = 250)
    @NotNull
    @Nationalized
    @Column(name = "email", nullable = false, length = 250)
    private String email;

    @NotNull
    @Column(name = "salary", nullable = false, precision = 18, scale = 3)
    private BigDecimal salary;

    @NotNull
    @Column(name = "hire_date", nullable = false)
    private Date hireDate;

    @Size(max = 50)
    @Nationalized
    @Column(name = "country", length = 50)
    private String country;

    @Size(max = 100)
    @Nationalized
    @Column(name = "province", length = 100)
    private String province;

    @Size(max = 100)
    @Nationalized
    @Column(name = "district", length = 100)
    private String district;

    @Size(max = 100)
    @Nationalized
    @Column(name = "ward", length = 100)
    private String ward;

    @Size(max = 250)
    @Nationalized
    @Column(name = "house_name", length = 250)
    private String houseName;

    @Column(name = "status")
    private Integer status;

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

}