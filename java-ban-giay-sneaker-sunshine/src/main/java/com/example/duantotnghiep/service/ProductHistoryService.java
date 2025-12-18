package com.example.duantotnghiep.service;

import com.example.duantotnghiep.dto.request.ProductHistoryRequest;
import com.example.duantotnghiep.dto.response.ProductHistoryResponse;

import java.util.List;

public interface ProductHistoryService {
    void create(ProductHistoryRequest request);
    void update(Long id, ProductHistoryRequest request);
    void delete(Long id);
    ProductHistoryResponse getById(Long id);
    List<ProductHistoryResponse> getAll();
    List<ProductHistoryResponse> findAllByProductId(Long productId);

}
