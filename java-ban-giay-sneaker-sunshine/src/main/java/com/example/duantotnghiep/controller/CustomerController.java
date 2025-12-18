package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.request.AddressCustomerRequest;
import com.example.duantotnghiep.dto.request.CustomerBlacklistRequest;
import com.example.duantotnghiep.dto.request.CustomerRequest;
import com.example.duantotnghiep.dto.response.AddressCustomerResponse;
import com.example.duantotnghiep.dto.response.BadCustomerResponse;
import com.example.duantotnghiep.dto.response.CustomerBlacklistHistoryResponse;
import com.example.duantotnghiep.dto.response.CustomerResponse;
import com.example.duantotnghiep.service.CustomerBlacklistHistoryService;
import com.example.duantotnghiep.service.CustomerService;
import com.example.duantotnghiep.xuatExcel.CustomerExcelExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;
    private final CustomerBlacklistHistoryService historyService;

    // CREATE
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@RequestBody CustomerRequest request) throws Exception{
        System.out.println("province: "+request.getProvinceName());
        System.out.println("district: "+request.getDistrictName());
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.ok(response);
    }

    // READ (get by id)
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        CustomerResponse response = customerService.getCustomerById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        List<CustomerResponse> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable Long id, @RequestBody CustomerRequest request) {
        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(response);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/phan-trang")
    public ResponseEntity<Page<CustomerResponse>> getCustomersByStatus(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<CustomerResponse> result = customerService.findCustomerByStatus( pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{customerId}/addresses")
    public ResponseEntity<List<AddressCustomerResponse>> getAllAddressesByCustomerId(@PathVariable Long customerId) {
        return ResponseEntity.ok(customerService.getByCustomerId(customerId));
    }

    @PostMapping("/{customerId}/addresses")
    public ResponseEntity<AddressCustomerResponse> createAddress(
            @PathVariable Long customerId,
            @RequestBody AddressCustomerRequest request
    ) {
        request.setCustomerId(customerId);
        return ResponseEntity.ok(customerService.create(request));
    }

    @PutMapping("/{customerId}/addresses/{addressId}")
    public ResponseEntity<AddressCustomerResponse> updateAddress(
            @PathVariable Long customerId,
            @PathVariable Long addressId,
            @RequestBody AddressCustomerRequest request
    ) {
        request.setCustomerId(customerId);
        return ResponseEntity.ok(customerService.update(addressId, request));
    }

    @GetMapping("/{customerId}/addresses/{addressId}")
    public ResponseEntity<AddressCustomerResponse> getAddressById(
            @PathVariable Long customerId,
            @PathVariable Long addressId
    ) {
        AddressCustomerResponse response = customerService.getAddressById(addressId);
        if (!response.getCustomerId().equals(customerId)) {
            return ResponseEntity.status(403).build(); // FORBIDDEN nếu không khớp
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{customerId}/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long customerId,
            @PathVariable Long addressId
    ) {
        AddressCustomerResponse response = customerService.getAddressById(addressId);
        if (!response.getCustomerId().equals(customerId)) {
            return ResponseEntity.status(403).build();
        }
        customerService.deleteAddressCustomer(addressId);
        return ResponseEntity.noContent().build();
    }


    @PutMapping("/{customerId}/addresses/{addressId}/set-default")
    public ResponseEntity<String> setDefaultAddress(
            @PathVariable Long customerId,
            @PathVariable Long addressId) {
        customerService.setDefaultAddress(customerId, addressId);
        return ResponseEntity.ok("Đã đặt địa chỉ mặc định thành công!");
    }

    @PutMapping("/{id}/blacklist")
    public ResponseEntity<?> blacklist(@PathVariable Long id, @RequestBody CustomerBlacklistRequest request) {
        customerService.blacklistCustomer(id, request);
        return ResponseEntity.ok("Đã cấm khách hàng.");
    }

    @PutMapping("/{id}/unblacklist")
    public ResponseEntity<?> unblacklist(@PathVariable Long id) {
        customerService.unblacklistCustomer(id);
        return ResponseEntity.ok("Đã gỡ cấm khách hàng.");
    }

    @GetMapping("/{customerId}/blacklist-history")
    public ResponseEntity<List<CustomerBlacklistHistoryResponse>> getHistoryByCustomerId(@PathVariable Long customerId) {
        return ResponseEntity.ok(historyService.getHistoryByCustomerId(customerId));
    }

    @GetMapping("/bad")
    public ResponseEntity<List<BadCustomerResponse>> getBlacklistedCustomers() {
        List<BadCustomerResponse> list = customerService.getAllBlacklistedCustomers();
        return ResponseEntity.ok(list);
    }

    ///Tìm kiếm nâng cao

    // CustomerController.java

    @GetMapping("/search")
    public ResponseEntity<Page<CustomerResponse>> searchByNameAndPhone(
            @RequestParam(required = false) String name,
            @RequestParam(required = false, name = "customerName") String customerName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String phoneSuffix,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdDate,desc") String sort
    ) {
        String finalName = firstNonBlank(customerName, name);
        String finalPhone = toNullIfBlank(phone);
        String finalSuffix = toNullIfBlank(phoneSuffix);

        if (finalName == null && finalPhone == null && finalSuffix == null) {
            return ResponseEntity.badRequest().body(Page.empty());
        }

        // sort
        String[] parts = sort.split(",");
        Sort s = (parts.length == 2)
                ? Sort.by(Sort.Direction.fromString(parts[1]), parts[0])
                : Sort.by(Sort.Direction.DESC, "createdDate");
        Pageable pageable = PageRequest.of(page, size, s);

        Page<CustomerResponse> result = customerService.searchByNameAndPhone(finalName, finalPhone, finalSuffix, pageable);
        return ResponseEntity.ok(result);

    }


    //xuất excel
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCustomers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone) throws IOException {

        var list = customerService.findAllForExport(name, phone); // <-- dùng method mới
        var in = CustomerExcelExporter.exportCustomerToExcel(list);
        byte[] bytes = in.readAllBytes();

        var headers = new org.springframework.http.HttpHeaders();
        headers.add(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=customers.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(bytes.length)
                .body(bytes);
    }


    private String toNullIfBlank(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v.trim();
            }
        }
        return null;
    }

}
