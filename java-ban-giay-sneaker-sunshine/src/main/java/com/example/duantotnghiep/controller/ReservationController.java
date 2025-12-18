package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.request.ReservationRequest;
import com.example.duantotnghiep.dto.response.ReservationResponse;
import com.example.duantotnghiep.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<String> create(@RequestBody ReservationRequest request) {
        reservationService.createReservation(request);
        return ResponseEntity.ok("Thêm mới thành công");
    }

}
