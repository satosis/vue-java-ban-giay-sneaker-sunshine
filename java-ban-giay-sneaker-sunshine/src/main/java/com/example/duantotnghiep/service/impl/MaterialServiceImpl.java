package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.model.Material;
import com.example.duantotnghiep.repository.MaterialRepository;
import com.example.duantotnghiep.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {
    private final MaterialRepository materialRepository;

    @Override
    public List<Material> getAll() {
        return materialRepository.findByStatus();
    }

    @Override
    public Material them(String name) {
        Material b = new Material();
        b.setMaterialCode(generateBrandCode());
        b.setMaterialName(name);
        b.setStatus(1);
        b.setCreatedDate(new Date());
        b.setCreatedBy("admin");
        Material saved = materialRepository.save(b);
        return saved;
    }

    @Override
    public Material sua(Long id, String name) {
        Material b = materialRepository.findById(id).orElse(null);
        b.setMaterialName(name);
        b.setUpdatedBy("admin");
        b.setUpdatedDate(new Date());
        Material updated= materialRepository.save(b);
        return updated;
    }

    @Override
    public void xoa(Long id) {
        Material b = materialRepository.findById(id).orElse(null);
        b.setUpdatedDate(new Date());
        b.setStatus(0);
        materialRepository.save(b);
    }

    private String generateBrandCode() {
        String prefix = "MT-";
        String datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String randomPart = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + datePart + "-" + randomPart;
    }
}
