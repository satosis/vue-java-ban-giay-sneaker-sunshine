package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.dto.request.SupplierRequest;
import com.example.duantotnghiep.dto.response.SupplierResponse;
import com.example.duantotnghiep.mapper.SupplierMapper;
import com.example.duantotnghiep.model.Supplier;
import com.example.duantotnghiep.repository.SupplierRepository;
import com.example.duantotnghiep.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {
    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    @Override
    public List<SupplierResponse> getAll() {
        return supplierRepository.findByStatus().stream()
                .map(supplierMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SupplierResponse them(SupplierRequest request) {
        Supplier supplier = supplierMapper.toEntity(request);
        supplier.setSupplierCode(generateCode());
        supplier.setSupplierStatus(1);
        supplier.setCreatedDate(new Date());
        supplier.setCreatedBy("admin"); // Hoặc lấy từ thông tin người dùng hiện tại
        Supplier savedSupplier = supplierRepository.save(supplier);
        return supplierMapper.toResponse(savedSupplier);
    }

    @Override
    public SupplierResponse sua(Long id, SupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id).orElse(null);
        if (supplier != null) {
            supplier.setSupplierName(request.getSupplierName());
            supplier.setCountry(request.getCountry());
            supplier.setProvince(request.getProvince());
            supplier.setDistrict(request.getDistrict());
            supplier.setWard(request.getWard());
            supplier.setHouseName(request.getHouseName());
            supplier.setUpdatedDate(new Date());
            supplier.setUpdatedBy("admin"); // Hoặc lấy từ thông tin người dùng hiện tại
            return supplierMapper.toResponse(supplierRepository.save(supplier));
        }
        return null;
    }

    @Override
    public void xoa(Long id) {
        Supplier supplier = supplierRepository.findById(id).orElse(null);
        if (supplier != null) {
            supplier.setUpdatedDate(new Date());
            supplier.setSupplierStatus(0); // Đánh dấu là không hoạt động
            supplierRepository.save(supplier);
        }
    }

    private String generateCode() {
        String prefix = "SUP-";
        String datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String randomPart = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + datePart + "-" + randomPart;
    }
}
