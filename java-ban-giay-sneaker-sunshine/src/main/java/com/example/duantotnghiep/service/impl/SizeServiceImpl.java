package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.model.Size;
import com.example.duantotnghiep.repository.SizeRepository;
import com.example.duantotnghiep.service.SizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
@Service
@RequiredArgsConstructor
public class SizeServiceImpl implements SizeService {

    private final SizeRepository sizeRepo;

    @Override
    public List<Size> getAll() {
        return sizeRepo.findByStatus();
    }

    @Override
    public Size them(String name) {
        Size b = new Size();
        b.setSizeCode(generateCode());
        b.setSizeName(name);
        b.setStatus(1);
        b.setCreatedDate(new Date());
        b.setCreatedBy("admin");
        Size saved = sizeRepo.save(b);
        return saved;
    }

    @Override
    public Size sua(Long id, String name) {
        Size b = sizeRepo.findById(id).orElse(null);
        b.setSizeName(name);
        b.setUpdatedBy("admin");
        b.setUpdatedDate(new Date());
        Size updated= sizeRepo.save(b);
        return updated;
    }

    @Override
    public void xoa(Long id) {
        Size b = sizeRepo.findById(id).orElse(null);
        b.setUpdatedDate(new Date());
        b.setStatus(0);
        sizeRepo.save(b);
    }

    private String generateCode() {
        String prefix = "SIZE-";
        String datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String randomPart = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + datePart + "-" + randomPart;
    }
}
