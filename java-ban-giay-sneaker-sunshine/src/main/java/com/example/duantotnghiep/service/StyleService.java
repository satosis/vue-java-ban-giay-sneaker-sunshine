package com.example.duantotnghiep.service;

import com.example.duantotnghiep.model.Style;

import java.util.List;

public interface StyleService {
    List<Style> getAll();
    Style them(String name);
    Style sua(Long id,String name);
    void xoa(Long id);
}
