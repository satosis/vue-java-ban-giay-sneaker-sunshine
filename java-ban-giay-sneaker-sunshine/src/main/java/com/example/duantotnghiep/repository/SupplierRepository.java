package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier,Long> {
    @Query("SELECT p FROM Supplier p where p.supplierStatus = 1 order by p.createdDate desc ")
    List<Supplier> findByStatus();
}
