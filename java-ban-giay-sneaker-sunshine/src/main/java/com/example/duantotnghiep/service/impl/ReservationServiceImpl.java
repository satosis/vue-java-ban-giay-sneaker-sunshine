package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.dto.request.ReservationRequest;
import com.example.duantotnghiep.dto.response.ReservationResponse;
import com.example.duantotnghiep.model.Customer;
import com.example.duantotnghiep.model.Reservation;
import com.example.duantotnghiep.model.User;
import com.example.duantotnghiep.repository.ReservationRepository;
import com.example.duantotnghiep.repository.UserRepository;
import com.example.duantotnghiep.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepo;

    @Override
    public List<ReservationResponse> findAll() {
        List<ReservationResponse> reservations = reservationRepository.findAllReservations();
        return reservations;
    }

    @Override
    public void createReservation(ReservationRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với username: " + username));

        Customer customer = user.getCustomer();
        if (customer == null) {
            throw new RuntimeException("Người dùng không phải là khách hàng.");
        }

        Reservation reservation = new Reservation();
        reservation.setCustomerId(customer.getId());
        reservation.setProductDetailId(request.getProductDetailId());
        reservation.setQuantity(request.getQuantity());
        reservation.setStatus(1);
        reservation.setCreatedAt(new Date());
        reservation.setUpdatedAt(new Date());
        reservationRepository.save(reservation);
    }

    @Override
    public void updateReservation(Long id, ReservationRequest request) {

    }

    @Override
    public void deleteReservation(Long id) {

    }
}
