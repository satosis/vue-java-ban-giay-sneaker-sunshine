package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<Material,Long> {
    @Query("SELECT p FROM Material p where p.status = 1 order by p.createdDate desc ")
    List<Material> findByStatus();

}
