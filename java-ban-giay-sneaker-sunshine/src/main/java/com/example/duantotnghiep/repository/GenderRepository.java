package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.model.Gender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GenderRepository extends JpaRepository<Gender,Long> {
    @Query("SELECT p FROM Gender p where p.status = 1 order by p.createdDate desc ")
    List<Gender> findByStatus(Integer status);


}
