package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZaloPayResponse {
    private String appTransId;
    private String orderUrl;
    private String zpTransToken;
    private int returnCode;
    private String returnMessage;

}

