package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.request.SupplierRequest;
import com.example.duantotnghiep.dto.response.SupplierResponse;
import com.example.duantotnghiep.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/supplier")
public class SupplierController {
    @Autowired
    private SupplierService supplierService;

    @GetMapping("/hien-thi")
    public ResponseEntity<List<SupplierResponse>> getAllSuppliers() {
        List<SupplierResponse> suppliers = supplierService.getAll();
        return ResponseEntity.ok(suppliers);
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> addSupplier(@RequestBody SupplierRequest request) {
        SupplierResponse newSupplier = supplierService.them(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(newSupplier);
    }

    // Cập nhật nhà cung cấp
    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponse> updateSupplier(@PathVariable Long id, @RequestBody SupplierRequest request) {
        SupplierResponse updatedSupplier = supplierService.sua(id, request);
        if (updatedSupplier == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedSupplier);
    }

    // Xóa nhà cung cấp
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        supplierService.xoa(id);
        return ResponseEntity.noContent().build();
    }
}
