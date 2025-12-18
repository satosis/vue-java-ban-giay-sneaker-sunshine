package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.request.QuenMatKhauReq;
import com.example.duantotnghiep.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/change-pass")
@RequiredArgsConstructor
public class QuenMatKhauController {

    private final CustomerService customerService;

    @PostMapping
    public void QuenMatKhauClient(@RequestBody QuenMatKhauReq req) {
        customerService.QuenMatKhauClient(req);
    }

}
