package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.model.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ColorRepository extends JpaRepository<Color,Long> {
    @Query("SELECT p FROM Color p where p.status = 1 order by p.createdDate desc ")
    List<Color> findByStatus();
}
