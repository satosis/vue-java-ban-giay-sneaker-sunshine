package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.request.ComplaintRequest;
import com.example.duantotnghiep.dto.response.ComplaintResponse;
import com.example.duantotnghiep.service.ComplainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {
    private final ComplainService complainService;

    @GetMapping
    public ResponseEntity<List<ComplaintResponse>> getAllComplaints() {
        List<ComplaintResponse> complaints = complainService.getAll();
        return ResponseEntity.ok(complaints);
    }

    @PostMapping
    public ResponseEntity<String> createComplaint(
            @ModelAttribute ComplaintRequest complaintRequest
    ) {
        complainService.addComplain(complaintRequest);
        return ResponseEntity.ok("Thêm khiếu nại thành công");
    }

    @PutMapping
    public ResponseEntity<String> updateComplaint(
            @ModelAttribute ComplaintRequest complaintRequest
    ) {
        complainService.updateComplain(complaintRequest);
        return ResponseEntity.ok("Cập nhật khiếu nại thành công");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteComplaint(@PathVariable Long id) {
        complainService.deleteComplain(id);
        return ResponseEntity.ok("Đã hủy khiếu nại");
    }
}
