package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.model.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SizeRepository extends JpaRepository<Size,Long> {

    @Query("SELECT p FROM Size p where p.status = 1 order by p.createdDate desc ")
    List<Size> findByStatus();

}
