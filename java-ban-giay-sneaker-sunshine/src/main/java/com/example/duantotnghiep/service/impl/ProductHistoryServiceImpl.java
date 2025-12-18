package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.dto.request.ProductHistoryRequest;
import com.example.duantotnghiep.dto.response.ProductHistoryResponse;
import com.example.duantotnghiep.mapper.ProductHistoryMapper;
import com.example.duantotnghiep.model.ProductHistory;
import com.example.duantotnghiep.repository.ProductHistoryRepository;
import com.example.duantotnghiep.service.ProductHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductHistoryServiceImpl implements ProductHistoryService {
    private final ProductHistoryRepository productHistoryRepository;
    private final ProductHistoryMapper mapper;
    @Override
    public void create(ProductHistoryRequest request) {
        ProductHistory entity = mapper.toEntity(request);
        productHistoryRepository.save(entity);
    }

    @Override
    public void update(Long id, ProductHistoryRequest request) {
        ProductHistory existing = productHistoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ProductHistory not found with id " + id));
        mapper.updateEntityFromRequest(request, existing);
        productHistoryRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        if (!productHistoryRepository.existsById(id)) {
            throw new RuntimeException("ProductHistory not found with id " + id);
        }
        productHistoryRepository.deleteById(id);
    }

    @Override
    public ProductHistoryResponse getById(Long id) {
        ProductHistory entity = productHistoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ProductHistory not found with id " + id));
        return mapper.toDto(entity);
    }

    @Override
    public List<ProductHistoryResponse> getAll() {
        List<ProductHistory> entities = productHistoryRepository.findAll();
        return mapper.toResponseList(entities);
    }

    @Override
    public List<ProductHistoryResponse> findAllByProductId(Long productId) {
        List<ProductHistoryResponse> res = productHistoryRepository.findAllByProductId(productId);

        if (res == null) {
            return new ArrayList<>();
        }
        return res;
    }
}
