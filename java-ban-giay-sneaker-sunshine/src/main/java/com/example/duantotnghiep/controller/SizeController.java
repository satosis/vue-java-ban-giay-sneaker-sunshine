package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.response.ProductResponse;
import com.example.duantotnghiep.model.Size;
import com.example.duantotnghiep.service.ProductService;
import com.example.duantotnghiep.service.SizeService;
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
@RequestMapping("/api/admin/size")
public class SizeController {
    @Autowired
    private SizeService service;
    @Autowired
    private ProductService productService;

    @GetMapping("/hien-thi")
    public ResponseEntity<List<Size>> getAll(){
        List<Size> list = service.getAll();
        return ResponseEntity.ok(list);
    }

    // Thêm kích thước
    @PostMapping
    public ResponseEntity<Size> addSize(@RequestParam String name) {
        Size newSize = service.them(name);
        return ResponseEntity.status(HttpStatus.CREATED).body(newSize);
    }

    // Cập nhật kích thước
    @PutMapping("/{id}")
    public ResponseEntity<Size> updateSize(@PathVariable Long id, @RequestParam String name) {
        Size updatedSize = service.sua(id, name);
        if (updatedSize == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedSize);
    }

    // Xóa kích thước
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSize(@PathVariable Long id) {
        service.xoa(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-size")
    public Page<ProductResponse> getProductsBySizeId(
            @RequestParam("sizeId") Long sizeId,
            Pageable pageable
    ) {
        return productService.getProductsBySizeId(sizeId, pageable);
    }

}
