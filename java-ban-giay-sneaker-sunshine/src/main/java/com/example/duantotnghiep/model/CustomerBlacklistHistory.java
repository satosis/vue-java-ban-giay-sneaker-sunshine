package com.example.duantotnghiep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "customer_blacklist_history")
public class CustomerBlacklistHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết với bảng Customer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "unblacklisted_by", length = 100)
    private String unblacklistedBy;

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;
}

