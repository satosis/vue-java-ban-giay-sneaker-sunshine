package com.example.duantotnghiep.dto.response;

import com.example.duantotnghiep.state.TrangThaiKhieuNai;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ComplaintResponse {
    private Long id;
    private Long invoiceId;
    private String invoiceCode;
    private Long customerId;
    private String customerName;
    private String reason;
    private TrangThaiKhieuNai status;
    private String description;
    private List<ComplainImageResponse> images;
    private String createdBy;
    private Date createdDate;
    private String updatedBy;
    private Date updatedDate;
}
