package com.example.duantotnghiep.service;

import com.example.duantotnghiep.dto.request.ReservationRequest;
import com.example.duantotnghiep.dto.response.ReservationResponse;

import java.util.List;

public interface ReservationService {
    List<ReservationResponse> findAll();
    void createReservation(ReservationRequest request);
    void updateReservation(Long id,ReservationRequest request);
    void  deleteReservation(Long id);

}
