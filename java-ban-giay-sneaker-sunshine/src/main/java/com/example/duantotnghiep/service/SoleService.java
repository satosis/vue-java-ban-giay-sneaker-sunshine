package com.example.duantotnghiep.service;

import com.example.duantotnghiep.model.Sole;

import java.util.List;

public interface SoleService {
    List<Sole> getAll();
    Sole them(String name);
    Sole sua(Long id,String name);
    void xoa(Long id);
}
