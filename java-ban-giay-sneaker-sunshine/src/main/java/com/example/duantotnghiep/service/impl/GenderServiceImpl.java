package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.dto.response.GenderDTO;
import com.example.duantotnghiep.mapper.GenderMapper;
import com.example.duantotnghiep.model.Gender;
import com.example.duantotnghiep.repository.GenderRepository;
import com.example.duantotnghiep.service.GenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GenderServiceImpl implements GenderService {
    private final GenderRepository genderRepository;
    private final GenderMapper genderMapper;

    @Override
    public Gender them(String name) {
        Gender b = new Gender();
        b.setGenderCode(generateBrandCode());
        b.setGenderName(name);
        b.setStatus(1);
        b.setCreatedDate(new Date());
        b.setCreatedBy("admin");
        Gender saved = genderRepository.save(b);
        return saved;
    }

    private String generateBrandCode() {
        String prefix = "GD-";
        String datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String randomPart = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + datePart + "-" + randomPart;
    }

    @Override
    public List<GenderDTO> getAll(Integer status) {
        List<Gender> list = (status != null)
                ? genderRepository.findByStatus(status)
                : genderRepository.findAll();

        return list.stream()
                .map(genderMapper::toDTO)
                .toList();
    }

}
