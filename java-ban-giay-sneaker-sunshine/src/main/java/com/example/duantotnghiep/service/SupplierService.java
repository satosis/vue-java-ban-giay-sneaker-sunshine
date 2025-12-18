package com.example.duantotnghiep.service;

import com.example.duantotnghiep.dto.request.SupplierRequest;
import com.example.duantotnghiep.dto.response.SupplierResponse;

import java.util.List;

public interface SupplierService {
    List<SupplierResponse> getAll();
    SupplierResponse them(SupplierRequest request);
    SupplierResponse sua(Long id,SupplierRequest request);
    void xoa(Long id);
}
