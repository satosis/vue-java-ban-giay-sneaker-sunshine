package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.model.AddressCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<AddressCustomer,Long> {
    //lấy thông tin địa chỉ khách hàng ra file xuất excel
    List<AddressCustomer> findByCustomerIdInAndDefaultAddressTrueAndStatus(List<Long> customerIds, Integer status);
    List<AddressCustomer> findTop1ByCustomerIdOrderByCreatedDateDesc(Long customerId);


    List<AddressCustomer> findByCustomerIdAndStatusOrderByDefaultAddressDesc(Long customerId,Integer status);

    @Modifying
    @Query("UPDATE AddressCustomer ca SET ca.defaultAddress = false WHERE ca.customer.id = :customerId")
    void clearDefaultAddress(@Param("customerId") Long customerId);

    //lấy bản ghi mới nhất
    @Query(value = """
    SELECT * FROM (
        SELECT a.*,
               ROW_NUMBER() OVER (
                   PARTITION BY a.customer_id
                   ORDER BY a.default_address DESC, a.created_date DESC
               ) AS rn
        FROM address_customer a
        WHERE a.customer_id IN (:ids)
          AND a.status = 1
    ) x
    WHERE x.rn = 1
    """, nativeQuery = true)
    List<AddressCustomer> findBestAddressesForCustomerIds(@Param("ids") List<Long> ids);


    AddressCustomer findFirstByCustomerIdAndDefaultAddressTrue(Long customerId);

}
