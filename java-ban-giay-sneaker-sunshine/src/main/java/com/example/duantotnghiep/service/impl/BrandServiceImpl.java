package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.model.Brand;
import com.example.duantotnghiep.repository.BrandRepository;
import com.example.duantotnghiep.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {
    private final BrandRepository brandRepository;

    @Override
    public List<Brand> getAll() {
        return brandRepository.findByStatus();
    }

    @Override
    public Brand them(String name) {
        Brand b = new Brand();
        b.setBrandCode(generateBrandCode());
        b.setBrandName(name);
        b.setStatus(1);
        b.setCreatedDate(new Date());
        b.setCreatedBy("admin");
        Brand savedBrand = brandRepository.save(b);
        return savedBrand;
    }

    @Override
    public Brand sua(Long id, String name) {
        Brand b = brandRepository.findById(id).orElse(null);
        b.setBrandName(name);
        b.setUpdatedBy("admin");
        b.setUpdatedDate(new Date());
        Brand updatedBrand = brandRepository.save(b);
        return updatedBrand;
    }

    @Override
    public void xoa(Long id) {
        Brand b = brandRepository.findById(id).orElse(null);
        b.setUpdatedDate(new Date());
        b.setStatus(0);
        brandRepository.save(b);
    }

    private String generateBrandCode() {
        String prefix = "Br-";
        String datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String randomPart = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + datePart + "-" + randomPart;
    }

}
