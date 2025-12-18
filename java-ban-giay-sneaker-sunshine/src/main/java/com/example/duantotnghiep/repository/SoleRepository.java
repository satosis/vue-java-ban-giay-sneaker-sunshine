package com.example.duantotnghiep.repository;


import com.example.duantotnghiep.model.Sole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SoleRepository extends JpaRepository<Sole,Long> {
    @Query("SELECT p FROM Sole p where p.status = 1 order by p.createdDate desc ")
    List<Sole> findByStatus();
}
