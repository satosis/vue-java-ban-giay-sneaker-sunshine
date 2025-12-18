package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmailAndStatus(String email,Integer status);
    boolean existsByPhoneAndStatus(String phone,Integer status);

    @Query("""
        select e from Employee e where e.status = 1 
                """)
    List<Employee> getData();

    @Query("""
        SELECT e
        FROM Employee e
        WHERE
            ( :employeeCode IS NOT NULL AND LOWER(e.employeeCode) LIKE CONCAT('%', LOWER(:employeeCode), '%') )
         OR ( :employeeName IS NOT NULL AND LOWER(e.employeeName) LIKE CONCAT('%', LOWER(:employeeName), '%') )
         OR ( :email        IS NOT NULL AND LOWER(e.email)        LIKE CONCAT('%', LOWER(:email), '%') )
        """)
    List<Employee> searchEmployees(
            @Param("employeeCode") String employeeCode,
            @Param("employeeName") String employeeName,
            @Param("email") String email
    );

}