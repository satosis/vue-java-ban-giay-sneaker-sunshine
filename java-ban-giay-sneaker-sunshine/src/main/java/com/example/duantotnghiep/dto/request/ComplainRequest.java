package com.example.duantotnghiep.dto.request;

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
public class ComplainRequest {
    private Long invoiceId;
    private Long customerId;
    private String reason;
    private Integer status;
    private String result;
    private List<MultipartFile> images;
}
