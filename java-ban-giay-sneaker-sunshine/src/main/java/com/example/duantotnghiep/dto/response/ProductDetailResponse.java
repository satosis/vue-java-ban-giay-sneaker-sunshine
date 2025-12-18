package com.example.duantotnghiep.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class ProductDetailResponse {
    private Long id;
    private Long sizeId;
    private Long brandId;
    private String brandName;
    private Long productId;
    private String productName;
    private String sizeName;
    private Long colorId;
    private String colorName;
    private String productDetailCode;
    private BigDecimal sellPrice;
    private BigDecimal discountedPrice;       // thêm
    private Integer discountPercentage;       // thêm
    private Long discountCampaignId; // <- Thêm dòng này
    private Integer quantity;
    private String description;
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date createdDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date updatedDate;
    private String createdBy;
    private String updatedBy;

    // Thêm trường này để dùng làm barcode
    private String barcode;

}
