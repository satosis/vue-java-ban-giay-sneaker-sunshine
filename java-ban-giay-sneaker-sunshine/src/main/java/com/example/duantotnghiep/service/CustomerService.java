package com.example.duantotnghiep.service;

import com.example.duantotnghiep.dto.request.AddressCustomerRequest;
import com.example.duantotnghiep.dto.request.CustomerBlacklistRequest;
import com.example.duantotnghiep.dto.request.CustomerRequest;
import com.example.duantotnghiep.dto.request.QuenMatKhauReq;
import com.example.duantotnghiep.dto.response.AddressCustomerResponse;
import com.example.duantotnghiep.dto.response.BadCustomerResponse;
import com.example.duantotnghiep.dto.response.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomerService {
    CustomerResponse createCustomer(CustomerRequest request) throws Exception;
    CustomerResponse updateCustomer(Long id, CustomerRequest request);
    void deleteCustomer(Long id);
    CustomerResponse getCustomerById(Long id);
    List<CustomerResponse> getAllCustomers();
    Page<CustomerResponse> findCustomerByStatus( Pageable pageable);

    List<AddressCustomerResponse> getByCustomerId(Long customerId);

    AddressCustomerResponse create(AddressCustomerRequest request);
    AddressCustomerResponse getAddressById(Long id);

    AddressCustomerResponse update(Long id, AddressCustomerRequest request);

    void deleteAddressCustomer(Long id);
    void setDefaultAddress(Long customerId, Long addressId);

    void blacklistCustomer(Long id, CustomerBlacklistRequest request);

    void unblacklistCustomer(Long id);

    List<BadCustomerResponse> getAllBlacklistedCustomers();
    void QuenMatKhauClient(QuenMatKhauReq req);

    //Tìm kiếm nâng cao

    Page<CustomerResponse> searchByNameAndPhone(String name, String phone,String phoneSuffix, Pageable pageable);
    // xuất excel lấy ra thông tin khách hàng
    List<CustomerResponse> findAllForExport(String name, String phone);
}
