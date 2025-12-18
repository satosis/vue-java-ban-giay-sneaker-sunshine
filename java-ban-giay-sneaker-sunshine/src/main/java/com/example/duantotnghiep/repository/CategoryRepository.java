package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category,Long> {
    List<Category> findAllByIdInAndStatus(List<Long> ids, Integer status);

    @Query("SELECT c FROM Category c WHERE c.id IN :categoryIds")
    List<Category> findCategoriesByIds(@Param("categoryIds") List<Long> categoryIds);

    @Query("SELECT p FROM Category p where p.status = 1 order by p.createdDate desc ")
    List<Category> findByStatus();

    Optional<Category> findByCategoryNameIgnoreCase(String categoryName);


}
