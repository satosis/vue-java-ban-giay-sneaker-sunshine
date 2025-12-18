package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.model.ComplaintImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplaintImageRepository extends JpaRepository<ComplaintImage,Long> {
}
