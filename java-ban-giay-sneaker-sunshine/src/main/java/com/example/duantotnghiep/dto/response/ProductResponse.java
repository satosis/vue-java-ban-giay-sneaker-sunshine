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
public class ProductResponse {

    private Long id;
    private String productCode;
    private String productName;
    private String materialName;
    private Long materialId;
    private String brandName;
    private Long brandId;
    private String styleName;
    private Long styleId;
    private String genderName;
    private Long genderId;
    private String soleName;
    private Long soleId;
    private String supplierName;
    private Long supplierId;
    private BigDecimal originPrice;
    private BigDecimal sellPrice;
    private BigDecimal discountedPrice;
    private Integer discountPercentage;
    private Long discountCampaignId; // <- Thêm dòng này
    private Integer quantity;
    private String description;
    private Integer status;
    private BigDecimal weight;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date createdDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date updatedDate;

    private String createdBy;
    private String updatedBy;

    private List<String> categoryNames;
    private List<ProductDetailResponse> productDetails;
    private List<ProductImageResponse> productImages;
    private List<CategoryResponse> categories;

    public ProductResponse(Long id, String productName, BigDecimal sellPrice, List<ProductImageResponse> productImages) {
        this.id = id;
        this.productName = productName;
        this.sellPrice = sellPrice;
        this.productImages = productImages;
    }

}
