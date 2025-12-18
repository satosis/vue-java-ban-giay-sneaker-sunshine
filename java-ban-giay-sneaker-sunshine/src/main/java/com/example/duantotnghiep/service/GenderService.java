package com.example.duantotnghiep.service;

import com.example.duantotnghiep.dto.response.GenderDTO;
import com.example.duantotnghiep.model.Gender;

import java.util.List;

public interface GenderService {
    Gender them(String name);

    List<GenderDTO> getAll(Integer status);
}
