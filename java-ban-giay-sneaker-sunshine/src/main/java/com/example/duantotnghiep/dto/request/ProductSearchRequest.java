package com.example.duantotnghiep.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.annotation.Nullable;
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
public class ProductSearchRequest {

    @Nullable
    private List<Long> categoryIds;

    @Nullable
    private String keyword;

    @Nullable
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date createdFrom;

    @Nullable
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date createdTo;
    
    @Nullable
    private Long brandId;

    @Nullable
    private Long genderId;

    @Nullable
    private Long styleId;

    @Nullable
    private Long soleId;

    @Nullable
    private Long materialId;

    @Nullable
    private BigDecimal priceMin;

    @Nullable
    private BigDecimal priceMax;

    @Nullable
    private Long colorId;

    @Nullable
    private Long sizeId;

    @Nullable
    private Integer page;

    @Nullable
    private Integer size;
}
