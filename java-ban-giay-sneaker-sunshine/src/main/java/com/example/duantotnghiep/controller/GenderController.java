package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.response.GenderDTO;
import com.example.duantotnghiep.dto.response.ProductResponse;
import com.example.duantotnghiep.model.Gender;
import com.example.duantotnghiep.service.GenderService;
import com.example.duantotnghiep.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/gender")
public class GenderController {
    @Autowired
    private GenderService genderService;
    @Autowired
    private ProductService productService;

    @GetMapping("/hien-thi")
    public ResponseEntity<List<GenderDTO>> hienThi(
            @RequestParam(value = "status", required = false) Integer status
    ) {
        return ResponseEntity.ok(genderService.getAll(status));
    }

    @GetMapping("/by-gender")
    public Page<ProductResponse> getProductsByGenderId(
            @RequestParam("genderId") Long genderId,
            Pageable pageable
    ) {
        return productService.getProductsByGenderId(genderId, pageable);
    }
}
