package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ComplainImageResponse {
    private Long id;
    private Long complaintId;
    private String fileName;
    private String fileType;
    private byte[] imageData;
}
