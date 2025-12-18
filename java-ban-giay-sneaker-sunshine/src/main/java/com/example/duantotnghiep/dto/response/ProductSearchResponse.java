package com.example.duantotnghiep.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductSearchResponse {

    // Thông tin cơ bản
    private Long id;
    private String productCode;
    private String productName;
    private String materialName;
    private String brandName;
    private String styleName;
    private String genderName;
    private String soleName;
    private String supplierName;

    // Giá & số lượng
    private BigDecimal sellPrice;
    private Integer quantity;

    // Giảm giá
    private Integer discountPercentage;
    private BigDecimal discountedPrice;
    private Long discountCampaignId; // <- Thêm dòng này

    // Mô tả & trọng lượng
    private String description;
    private BigDecimal weight;

    // Trạng thái & metadata
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date createdDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date updatedDate;
    private String createdBy;
    private String updatedBy;

    // Danh sách liên quan
    private List<String> categoryNames;
    private List<ProductDetailResponse> productDetails;
    private List<ProductImageResponse> productImages;
}

