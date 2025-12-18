package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.model.Sole;
import com.example.duantotnghiep.repository.SoleRepository;
import com.example.duantotnghiep.service.SoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SoleServiceImpl implements SoleService {
    private final SoleRepository soleRepository;

    @Override
    public List<Sole> getAll() {
        return soleRepository.findByStatus();
    }

    @Override
    public Sole them(String name) {
        Sole b = new Sole();
        b.setSoleCode(generateCode());
        b.setSoleName(name);
        b.setStatus(1);
        b.setCreatedDate(new Date());
        b.setCreatedBy("admin");
        Sole saved = soleRepository.save(b);
        return saved;
    }

    @Override
    public Sole sua(Long id, String name) {
        Sole b = soleRepository.findById(id).orElse(null);
        b.setSoleName(name);
        b.setUpdatedBy("admin");
        b.setUpdatedDate(new Date());
        Sole updated= soleRepository.save(b);
        return updated;
    }

    @Override
    public void xoa(Long id) {
        Sole b = soleRepository.findById(id).orElse(null);
        b.setUpdatedDate(new Date());
        b.setStatus(0);
        soleRepository.save(b);
    }

    private String generateCode() {
        String prefix = "SOLE-";
        String datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String randomPart = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + datePart + "-" + randomPart;
    }
}
