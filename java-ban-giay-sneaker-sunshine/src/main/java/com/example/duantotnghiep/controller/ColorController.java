package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.response.ProductResponse;
import com.example.duantotnghiep.model.Color;
import com.example.duantotnghiep.service.ColorService;
import com.example.duantotnghiep.service.ProductService;
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
@RequestMapping("/api/admin/color")
public class ColorController {

    @Autowired
    private ColorService service;
    @Autowired
    private ProductService productService;

    @GetMapping("/hien-thi")
    public ResponseEntity<List<Color>> getAll(){
        List<Color> list = service.getAll();
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<Color> addColor(@RequestParam String name) {
        Color newColor = service.them(name);
        return ResponseEntity.status(HttpStatus.CREATED).body(newColor);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Color> updateColor(@PathVariable Long id, @RequestParam String name) {
        Color updatedColor = service.sua(id, name);
        if (updatedColor == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedColor);
    }

    // Xóa màu sắc
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteColor(@PathVariable Long id) {
        service.xoa(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-color")
    public Page<ProductResponse> getProductsByColorId(
            @RequestParam("colorId") Long colorId,
            Pageable pageable
    ) {
        return productService.getProductsByColorId(colorId, pageable);
    }


}
