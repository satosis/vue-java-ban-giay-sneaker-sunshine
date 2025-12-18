package com.example.duantotnghiep.service;


import com.example.duantotnghiep.model.Color;

import java.util.List;

public interface ColorService {
    List<Color> getAll();
    Color them(String name);
    Color sua(Long id,String name);
    void xoa(Long id);
}
