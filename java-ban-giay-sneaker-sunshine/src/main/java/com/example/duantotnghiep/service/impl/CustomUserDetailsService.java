package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        com.example.duantotnghiep.model.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng"));

        String role;
        Integer userRole = user.getRole();
        if (userRole != null && userRole == 1) {
            role = "ADMIN"; // sẽ thành ROLE_ADMIN tự động
        } else if (userRole != null && userRole == 2) {
            role = "STAFF"; // sẽ thành ROLE_STAFF tự động
        } else if (userRole != null && userRole == 3) {
            role = "USER";  // thêm case 3 cho USER
        } else {
            throw new RuntimeException("Vai trò không hợp lệ");
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(role) // sẽ tự động thêm tiền tố "ROLE_"
                .build();
    }
}

