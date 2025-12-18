package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.model.Material;
import com.example.duantotnghiep.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/api/admin/material")
public class MaterialController {
    @Autowired
    private MaterialService service;

    @GetMapping("/hien-thi")
    public ResponseEntity<List<Material>> getAll(){
        List<Material> list = service.getAll();
        return ResponseEntity.ok(list);
    }

    // Thêm vật liệu
    @PostMapping
    public ResponseEntity<Material> addMaterial(@RequestParam String name) {
        Material newMaterial = service.them(name);
        return ResponseEntity.status(HttpStatus.CREATED).body(newMaterial);
    }

    // Cập nhật vật liệu
    @PutMapping("/{id}")
    public ResponseEntity<Material> updateMaterial(@PathVariable Long id, @RequestParam String name) {
        Material updatedMaterial = service.sua(id, name);
        if (updatedMaterial == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedMaterial);
    }

    // Xóa vật liệu
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaterial(@PathVariable Long id) {
        service.xoa(id);
        return ResponseEntity.noContent().build();
    }
}
