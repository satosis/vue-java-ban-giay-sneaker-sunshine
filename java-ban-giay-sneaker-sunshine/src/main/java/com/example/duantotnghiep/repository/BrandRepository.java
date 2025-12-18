package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.model.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrandRepository extends JpaRepository<Brand,Long> {
    @Query("SELECT p FROM Brand p where p.status = 1 order by p.createdDate desc ")
    List<Brand> findByStatus();

}
