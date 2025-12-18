package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.model.Style;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StyleRepository extends JpaRepository<Style,Long> {
    @Query("SELECT p FROM Style p where p.status = 1 order by p.createdDate desc ")
    List<Style> findByStatus();
}
