package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.model.Style;
import com.example.duantotnghiep.service.StyleService;
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
@RequestMapping("/api/admin/style")
public class StyleController {
    @Autowired
    private StyleService styleService;

    @GetMapping("/hien-thi")
    public ResponseEntity<List<Style>> getAll(){
        List<Style> list = styleService.getAll();
        return ResponseEntity.ok(list);
    }

    // Thêm kiểu dáng
    @PostMapping
    public ResponseEntity<Style> addStyle(@RequestParam String name) {
        Style newStyle = styleService.them(name);
        return ResponseEntity.status(HttpStatus.CREATED).body(newStyle);
    }

    // Cập nhật kiểu dáng
    @PutMapping("/{id}")
    public ResponseEntity<Style> updateStyle(@PathVariable Long id, @RequestParam String name) {
        Style updatedStyle = styleService.sua(id, name);
        if (updatedStyle == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedStyle);
    }

    // Xóa kiểu dáng
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStyle(@PathVariable Long id) {
        styleService.xoa(id);
        return ResponseEntity.noContent().build();
    }
}
