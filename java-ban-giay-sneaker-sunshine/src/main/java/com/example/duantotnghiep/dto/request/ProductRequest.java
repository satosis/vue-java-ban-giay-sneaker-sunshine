    package com.example.duantotnghiep.dto.request;

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
    public class ProductRequest {
        private String productCode;
        private String productName;
        private Long materialId;
        private Long brandId;
        private Long styleId;
        private Long genderId;
        private Long soleId;
        private Long supplierId;
        private BigDecimal originPrice;
        private BigDecimal sellPrice;
        private Integer quantity;
        private BigDecimal weight;
        private String description;
        private Integer status;
        private Date createdDate;
        private Date modifiedDate;
        private String createdBy;
        private String modifiedBy;
        private List<ProductImageRequest> productImages;
        private List<Long> categoryIds;
        private List<Long> oldImageIds;
        private List<Long> oldColorIds;
        private List<ProductDetailRequest> productDetails;
    }
    
