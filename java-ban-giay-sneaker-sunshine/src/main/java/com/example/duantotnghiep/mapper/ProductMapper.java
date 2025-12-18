package com.example.duantotnghiep.mapper;

import com.example.duantotnghiep.dto.request.ProductRequest;
import com.example.duantotnghiep.dto.response.ProductResponse;
import com.example.duantotnghiep.dto.response.ProductSearchResponse;
import com.example.duantotnghiep.model.Brand;
import com.example.duantotnghiep.model.Gender;
import com.example.duantotnghiep.model.Material;
import com.example.duantotnghiep.model.Product;
import com.example.duantotnghiep.model.ProductCategory;
import com.example.duantotnghiep.model.Sole;
import com.example.duantotnghiep.model.Style;
import com.example.duantotnghiep.model.Supplier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(
        componentModel = "spring",
        uses = {CategoryMapper.class, ProductImageMapper.class, ProductDetailMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProductMapper {

    @Mapping(target = "materialName", source = "material.materialName")
    @Mapping(target = "brandName", source = "brand.brandName")
    @Mapping(target = "styleName", source = "style.styleName")
    @Mapping(target = "genderName", source = "gender.genderName")
    @Mapping(target = "soleName", source = "sole.soleName")
    @Mapping(target = "materialId", source = "material.id")
    @Mapping(target = "brandId", source = "brand.id")
    @Mapping(target = "styleId", source = "style.id")
    @Mapping(target = "genderId", source = "gender.id")
    @Mapping(target = "soleId", source = "sole.id")
    @Mapping(target = "supplierName", source = "supplier.supplierName")
    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "categoryNames", source = "productCategories")
    ProductResponse toResponse(Product product);

    @Mapping(target = "material.id", source = "materialId")
    @Mapping(target = "brand.id", source = "brandId")
    @Mapping(target = "style.id", source = "styleId")
    @Mapping(target = "gender.id", source = "genderId")
    @Mapping(target = "sole.id", source = "soleId")
    @Mapping(target = "supplier.id", source = "supplierId")
    @Mapping(target = "productImages", ignore = true)
    @Mapping(target = "productCategories", ignore = true)
    @Mapping(target = "productDetails", ignore = true)
    Product toEntity(ProductRequest request);

    @Mapping(target = "materialName", source = "material.materialName")
    @Mapping(target = "brandName", source = "brand.brandName")
    @Mapping(target = "styleName", source = "style.styleName")
    @Mapping(target = "genderName", source = "gender.genderName")
    @Mapping(target = "soleName", source = "sole.soleName")
    @Mapping(target = "supplierName", source = "supplier.supplierName")
    @Mapping(target = "categoryNames", source = "productCategories")
    @Mapping(target = "productDetails", source = "productDetails")
    ProductSearchResponse toResponseSearch(Product product);


    @Mapping(target = "material", source = "materialId")
    @Mapping(target = "brand", source = "brandId")
    @Mapping(target = "style", source = "styleId")
    @Mapping(target = "gender", source = "genderId")
    @Mapping(target = "sole", source = "soleId")
    @Mapping(target = "supplier", source = "supplierId")
    void updateEntityFromRequest(ProductRequest request, @MappingTarget Product product);

    // Helper method chuyển Long -> Material
    default Material mapMaterial(Long id) {
        if (id == null) {
            return null;
        }
        Material material = new Material();
        material.setId(id);
        return material;
    }

    default Supplier mapSupplier(Long id) {
        if (id == null) {
            return null;
        }
        Supplier supplier = new Supplier();
        supplier.setId(id);
        return supplier;
    }


    // Helper method chuyển Long -> Brand
    default Brand mapBrand(Long id) {
        if (id == null) {
            return null;
        }
        Brand brand = new Brand();
        brand.setId(id);
        return brand;
    }

    // Helper method chuyển Long -> Style
    default Style mapStyle(Long id) {
        if (id == null) {
            return null;
        }
        Style style = new Style();
        style.setId(id);
        return style;
    }

    // Helper method chuyển Long -> Gender
    default Gender mapGender(Long id) {
        if (id == null) {
            return null;
        }
        Gender gender = new Gender();
        gender.setId(id);
        return gender;
    }

    // Helper method chuyển Long -> Sole
    default Sole mapSole(Long id) {
        if (id == null) {
            return null;
        }
        Sole sole = new Sole();
        sole.setId(id);
        return sole;
    }

    default List<String> mapCategoryNames(List<ProductCategory> productCategories) {
        if (productCategories == null) {
            return Collections.emptyList();
        }
        return productCategories.stream()
                .map(pc -> pc.getCategory().getCategoryName())
                .collect(Collectors.toList());
    }

}
