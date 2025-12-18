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
public class FavoriteProductResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long productId;
    private Integer rate;
    private String comment;
    private Date createdAt;
}
