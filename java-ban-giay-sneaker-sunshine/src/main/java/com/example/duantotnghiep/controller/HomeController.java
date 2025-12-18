package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.response.ReservationResponse;
import com.example.duantotnghiep.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/pre-orders")
@RequiredArgsConstructor
public class HomeController {
     private final ReservationService reservationService;

    @GetMapping("/get-all")
    public ResponseEntity<List<ReservationResponse>> getAll() {
        List<ReservationResponse> list = reservationService.findAll();
        return ResponseEntity.ok(list);
    }

}
