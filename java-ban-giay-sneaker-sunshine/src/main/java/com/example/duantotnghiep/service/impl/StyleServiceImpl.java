package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.model.Style;
import com.example.duantotnghiep.repository.StyleRepository;
import com.example.duantotnghiep.service.StyleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StyleServiceImpl implements StyleService {
    private final StyleRepository styleRepository;

    @Override
    public List<Style> getAll() {
        return styleRepository.findByStatus();
    }

    @Override
    public Style them(String name) {
        Style b = new Style();
        b.setStyleCode(generateCode());
        b.setStyleName(name);
        b.setStatus(1);
        b.setCreatedDate(new Date());
        b.setCreatedBy("admin");
        Style saved= styleRepository.save(b);
        return saved;
    }

    @Override
    public Style sua(Long id, String name) {
        Style b = styleRepository.findById(id).orElse(null);
        b.setStyleName(name);
        b.setUpdatedBy("admin");
        b.setUpdatedDate(new Date());
        Style updated= styleRepository.save(b);
        return updated;
    }

    @Override
    public void xoa(Long id) {
        Style b = styleRepository.findById(id).orElse(null);
        b.setUpdatedDate(new Date());
        b.setStatus(0);
        styleRepository.save(b);
    }

    private String generateCode() {
        String prefix = "STYLE-";
        String datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String randomPart = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + datePart + "-" + randomPart;
    }
}
