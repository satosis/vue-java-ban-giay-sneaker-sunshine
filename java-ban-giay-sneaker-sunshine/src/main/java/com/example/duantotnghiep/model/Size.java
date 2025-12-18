package com.example.duantotnghiep.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "\"size\"")
public class Size {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @jakarta.validation.constraints.Size(max = 50)
    @Column(name = "size_code", length = 50)
    private String sizeCode;

    @jakarta.validation.constraints.Size(max = 250)
    @NotNull
    @Nationalized
    @Column(name = "size_name", nullable = false, length = 250)
    private String sizeName;

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