package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.model.Sole;
import com.example.duantotnghiep.service.SoleService;
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
@RequestMapping("/api/admin/sole")
public class SoleController {
    @Autowired
    private SoleService service;

    @GetMapping("/hien-thi")
    public ResponseEntity<List<Sole>> getAll(){
        List<Sole> list = service.getAll();
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<Sole> addSole(@RequestParam String name) {
        Sole newSole = service.them(name);
        return ResponseEntity.status(HttpStatus.CREATED).body(newSole);
    }

    // Cập nhật loại đế
    @PutMapping("/{id}")
    public ResponseEntity<Sole> updateSole(@PathVariable Long id, @RequestParam String name) {
        Sole updatedSole = service.sua(id, name);
        if (updatedSole == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedSole);
    }

    // Xóa loại đế
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSole(@PathVariable Long id) {
        service.xoa(id);
        return ResponseEntity.noContent().build();
    }
}
