package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReservationResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long productDetailId;
    private String productName;
    private String colorName;
    private String sizeName;
    private String phone;
    private String email;
    private Integer quantity;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
}
