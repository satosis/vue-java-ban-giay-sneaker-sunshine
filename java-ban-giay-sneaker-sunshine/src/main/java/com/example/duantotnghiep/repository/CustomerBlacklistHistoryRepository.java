package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.model.CustomerBlacklistHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerBlacklistHistoryRepository extends JpaRepository<CustomerBlacklistHistory, Long> {
    List<CustomerBlacklistHistory> findByCustomerId(Long customerId);

    @Query("""
           SELECT h
           FROM CustomerBlacklistHistory h
           WHERE h.customer.id = :customerId
             AND h.endTime IS NULL
           """)
    List<CustomerBlacklistHistory> findOpenHistoriesByCustomer(@Param("customerId") Long customerId);
}

