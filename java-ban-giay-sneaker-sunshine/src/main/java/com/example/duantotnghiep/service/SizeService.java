package com.example.duantotnghiep.service;

import com.example.duantotnghiep.model.Size;

import java.util.List;

public interface SizeService {
    List<Size> getAll();
    Size them(String name);
    Size sua(Long id,String name);
    void xoa(Long id);
}
