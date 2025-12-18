package com.example.duantotnghiep.dto.request;

import com.example.duantotnghiep.state.TrangThaiKhieuNai;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ComplaintRequest {
    private Long id;
    private Long invoiceId;
    private Long customerId;
    private String reason;
    private TrangThaiKhieuNai status;
    private String description;
    private List<MultipartFile> files;
}
