package com.example.duantotnghiep.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;
import org.hibernate.validator.constraints.UniqueElements;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 50)
    @Column(name = "customer_code", length = 50)
    private String customerCode;

    @Size(max = 255)
    @Nationalized
    @Column(name = "customer_name")
    private String customerName;

    @Size(max = 255)
    @Nationalized
    @Column(name = "email")
    private String email;

    @Size(max = 50)
    @Nationalized
    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "gender")
    private Integer gender;

    @Column(name = "date_of_birth")
    private Date dateOfBirth;

    @Column(name = "status")
    private Integer status;

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

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AddressCustomer> addressList;

    @Column(name = "is_blacklisted")
    private Boolean isBlacklisted = false;

    @Size(max = 255)
    @Nationalized
    @Column(name = "blacklist_reason")
    private String blacklistReason;

    @Column(name = "blacklist_expiry_date")
    private LocalDateTime blacklistExpiryDate;

    @Column(name = "trust_score")
    private Integer trustScore = 100;

    @Column(name = "last_blacklist_checked")
    private LocalDateTime lastBlacklistChecked;

    @Column(name = "last_blacklist_cancel_count")
    private Integer lastBlacklistCancelCount;
}
