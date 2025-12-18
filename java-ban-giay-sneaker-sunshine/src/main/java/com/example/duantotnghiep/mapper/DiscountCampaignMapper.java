package com.example.duantotnghiep.mapper;

import com.example.duantotnghiep.dto.request.DiscountCampaignRequest;
import com.example.duantotnghiep.dto.response.DiscountCampaignProductDetailResponse;
import com.example.duantotnghiep.dto.response.DiscountCampaignProductResponse;
import com.example.duantotnghiep.dto.response.DiscountCampaignResponse;
import com.example.duantotnghiep.model.DiscountCampaign;
import com.example.duantotnghiep.model.DiscountCampaignProduct;
import com.example.duantotnghiep.model.DiscountCampaignProductDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DiscountCampaignMapper {

    // Các mapping khác nếu cần
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.productName", target = "productName")
    @Mapping(source = "product.productCode", target = "productCode")   // <<-- thêm dòng này
    DiscountCampaignProductResponse toProductResponse(DiscountCampaignProduct entity);

    @Mapping(source = "productDetail.id", target = "productDetailId")
    DiscountCampaignProductDetailResponse toProductDetailResponse(DiscountCampaignProductDetail entity);

    DiscountCampaignResponse toResponse(DiscountCampaign entity);

    @Mapping(target = "products", ignore = true)
    @Mapping(target = "productDetails", ignore = true)
    DiscountCampaign toEntity(DiscountCampaignRequest dto);
}












