package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.request.ProductHistoryRequest;
import com.example.duantotnghiep.dto.response.ProductHistoryResponse;
import com.example.duantotnghiep.service.ProductHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/product-histories")
@RequiredArgsConstructor
public class ProductHistoryController {
    private final ProductHistoryService productHistoryService;

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody ProductHistoryRequest request) {
        productHistoryService.create(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody ProductHistoryRequest request) {
        productHistoryService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productHistoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductHistoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productHistoryService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ProductHistoryResponse>> getAll() {
        return ResponseEntity.ok(productHistoryService.getAll());
    }

    @GetMapping("/get-by-productId")
    public ResponseEntity<List<ProductHistoryResponse>> getAllByProductId(@RequestParam("productId") Long productId) {
        return ResponseEntity.ok(productHistoryService.findAllByProductId(productId));
    }

}
