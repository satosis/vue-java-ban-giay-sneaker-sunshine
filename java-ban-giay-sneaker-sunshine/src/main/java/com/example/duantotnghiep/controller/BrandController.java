package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.response.ProductResponse;
import com.example.duantotnghiep.model.Brand;
import com.example.duantotnghiep.service.BrandService;
import com.example.duantotnghiep.service.ProductService;
import com.example.duantotnghiep.service.impl.ProductServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/brand")
public class BrandController {

    @Autowired
    private BrandService service;
    @Autowired
    private ProductService productService;
    @Autowired
    private ProductServiceImpl productServiceImpl;

    @GetMapping("/hien-thi")
    public ResponseEntity<List<Brand>> getAll(){
        List<Brand> list = service.getAll();
        return ResponseEntity.ok(list);
    }

    // Thêm thương hiệu
    @PostMapping
    public ResponseEntity<Brand> addBrand(@RequestParam String name) {
        Brand newBrand = service.them(name);
        return ResponseEntity.status(HttpStatus.CREATED).body(newBrand);
    }

    // Cập nhật thương hiệu
    @PutMapping("/{id}")
    public ResponseEntity<Brand> updateBrand(@PathVariable Long id, @RequestParam String name) {
        Brand updatedBrand = service.sua(id, name);
        if (updatedBrand == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedBrand);
    }

    // Xóa thương hiệu
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBrand(@PathVariable Long id) {
        service.xoa(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{brandId}/products")
    public Page<ProductResponse> getProductsOfBrand(@PathVariable Long brandId, Pageable pageable) {
        return productServiceImpl.getProductsByBrand(brandId, pageable);
    }

}

