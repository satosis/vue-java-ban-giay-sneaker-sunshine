package com.example.duantotnghiep.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductDetailRequest {
    private Long id;
    private Long sizeId;
    private Long colorId;
    private String productDetailCode;
    private BigDecimal sellPrice;
    private Integer quantity;
    private String description;
    private Integer status;
    private Date createdDate;
    private Date modifiedDate;
    private String createdBy;
    private String modifiedBy;
}
