package com.example.duantotnghiep.service;

import com.example.duantotnghiep.dto.request.ComplaintRequest;
import com.example.duantotnghiep.dto.response.ComplaintResponse;

import java.util.List;

public interface ComplainService {
    List<ComplaintResponse>  getAll();
    void addComplain(ComplaintRequest complaintRequest);
    void updateComplain(ComplaintRequest complaintRequest);
    void deleteComplain(Long id);
}
