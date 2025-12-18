package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.dto.response.ReservationResponse;
import com.example.duantotnghiep.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
        select new com.example.duantotnghiep.dto.response.ReservationResponse(
        rv.id,rv.customerId,c.customerName,rv.productDetailId,p.productName,pd.color.colorName,
        pd.size.sizeName,c.phone,c.email,rv.quantity,rv.status,rv.createdAt,rv.updatedAt
        ) from Reservation rv
        left join Customer c on c.id = rv.customerId and c.status = 1
        left join ProductDetail pd on pd.id = rv.productDetailId and pd.status = 1
        left join Product p on p.id = pd.product.id 
""")
    List<ReservationResponse> findAllReservations();
}
