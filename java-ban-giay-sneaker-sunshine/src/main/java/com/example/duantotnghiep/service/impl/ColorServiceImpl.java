package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.model.Color;
import com.example.duantotnghiep.repository.ColorRepository;
import com.example.duantotnghiep.service.ColorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ColorServiceImpl implements ColorService {
    private final ColorRepository colorRepository;

    @Override
    public List<Color> getAll() {
        return colorRepository.findByStatus();
    }

    @Override
    public Color them(String name) {
        Color b = new Color();
        b.setColorCode(generateBrandCode());
        b.setColorName(name);
        b.setStatus(1);
        b.setCreatedDate(new Date());
        b.setCreatedBy("admin");
        Color savedBrand = colorRepository.save(b);
        return savedBrand;
    }

    @Override
    public Color sua(Long id, String name) {
        Color b = colorRepository.findById(id).orElse(null);
        b.setColorName(name);
        b.setUpdatedBy("admin");
        b.setUpdatedDate(new Date());
        Color updated= colorRepository.save(b);
        return updated;
    }

    @Override
    public void xoa(Long id) {
        Color b = colorRepository.findById(id).orElse(null);
        b.setUpdatedDate(new Date());
        b.setStatus(0);
        colorRepository.save(b);
    }

    private String generateBrandCode() {
        String prefix = "CL-";
        String datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String randomPart = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + datePart + "-" + randomPart;
    }

}
