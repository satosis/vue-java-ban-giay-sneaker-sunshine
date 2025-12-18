package com.example.duantotnghiep.service;

import com.example.duantotnghiep.dto.request.ProductFilterRequest;
import com.example.duantotnghiep.dto.request.ProductRequest;
import com.example.duantotnghiep.dto.request.ProductSearchRequest;
import com.example.duantotnghiep.dto.response.*;
import com.example.duantotnghiep.model.ProductDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);
    ProductResponse restoreProduct(Long id);

    void deleteProduct(Long id);
    void deleteProductV2(Long id);

    ProductResponse getProductById(Long id);
    ProductResponse getProductByIdV2(Long id);


    Page<ProductDetailResponse> pageProductDetailsByProductId(
            int page, int size, Long productId, Long colorId, Long brandId);

    Page<ProductDetailResponse> pageProductDetails(int page, int size, List<Long> productIds);

    List<ProductDetailResponse> getProductDetailById(Long productId);

    Page<ProductResponse> getAllProducts(Pageable pageable);

    ProductDetailResponse scanProductDetail(String rawCode);

    PaginationDTO<ProductSearchResponse> phanTrang(ProductSearchRequest request, Pageable pageable);

    void exportProductToExcel(ProductSearchRequest dto, OutputStream outputStream) throws IOException;
    void exportProductToExcelByIds(List<Long> productIds, OutputStream outputStream) throws IOException;

    PaginationDTO<ProductSearchResponse> getProductRemoved(ProductSearchRequest request, Pageable pageable);

    List<ProductResponse> findProductWithImage();

    List<ProductResponse> findProducts(List<Long> productIds);
    List<FavoriteProductResponse> getFavoritesByProductId(Long productId);

    Page<ProductResponse> getProductsByBrand(Long brandId, Pageable pageable);

    Page<ProductResponse> getProductsByGenderId(Long genderId, Pageable pageable);

    Page<ProductResponse> getProductsByColorId(Long colorId, Pageable pageable);

    Page<ProductResponse> getProductsBySizeId(Long sizeId, Pageable pageable);

    List<ProductResponse> getProductSearch(String name);

    ProductDetailResponse verifyProductDetail(Long id);
    List<ProductDetailResponse> verifyListProductDetail(List<Long> id);

    @Transactional(readOnly = true)
    Page<ProductResponse> searchProducts(String keyword, Pageable pageable);
}
