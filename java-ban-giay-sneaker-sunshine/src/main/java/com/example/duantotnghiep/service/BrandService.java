package com.example.duantotnghiep.service;

import com.example.duantotnghiep.model.Brand;

import java.util.List;

public interface BrandService {
    List<Brand> getAll();
    Brand them(String name);
    Brand sua(Long id,String name);
    void xoa(Long id);
}
