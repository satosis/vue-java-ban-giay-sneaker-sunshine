package com.example.duantotnghiep.mapper;

import com.example.duantotnghiep.dto.request.ComplaintRequest;
import com.example.duantotnghiep.dto.response.ComplaintResponse;
import com.example.duantotnghiep.model.Complaint;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ComplaintMapper {
    ComplaintResponse complaintToComplaintResponse(Complaint complaint);

    Complaint toEntity(ComplaintRequest complaintRequest);
}
