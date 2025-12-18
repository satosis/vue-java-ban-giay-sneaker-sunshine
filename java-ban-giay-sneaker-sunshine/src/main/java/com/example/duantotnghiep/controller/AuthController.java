package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.request.CustomerRequest;
import com.example.duantotnghiep.dto.request.LoginRequest;
import com.example.duantotnghiep.dto.response.CustomerResponse;
import com.example.duantotnghiep.securiry.AuthService;
import com.example.duantotnghiep.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CustomerService customerService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<CustomerResponse> registerCustomer(@RequestBody CustomerRequest request) throws Exception{
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login-user") // Cái này chỉ cho USER (role = 3)
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginUserOnly(request)); // bạn tách logic tại đây
    }
}

