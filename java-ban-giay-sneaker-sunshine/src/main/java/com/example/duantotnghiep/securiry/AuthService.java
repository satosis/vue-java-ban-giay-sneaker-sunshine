package com.example.duantotnghiep.securiry;

import com.example.duantotnghiep.JWT.JwtUtil;
import com.example.duantotnghiep.dto.request.LoginRequest;
import com.example.duantotnghiep.dto.response.LoginResponse;
import com.example.duantotnghiep.dto.response.UserDTO;
import com.example.duantotnghiep.mapper.UserMapper;
import com.example.duantotnghiep.model.User;
import com.example.duantotnghiep.repository.UserRepository;
import com.example.duantotnghiep.service.impl.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        try {
            String username = request.getUsername();
            String rawPassword = request.getPassword();

            if (username == null || username.isBlank()) {
                throw new RuntimeException("❌ Tên đăng nhập không được để trống");
            }

            if (rawPassword == null || rawPassword.isBlank()) {
                throw new RuntimeException("❌ Mật khẩu không được để trống");
            }

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng"));

            // So sánh mật khẩu không mã hóa
            if (!rawPassword.equals(user.getPassword())) {
                throw new BadCredentialsException("Sai mật khẩu");
            }

            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, rawPassword)
            );

            String token = jwtUtil.generateToken((UserDetails) auth.getPrincipal());

            UserDTO userDTO = userMapper.toDto(user);
            String employeeName = userDTO.getEmployee() != null ? userDTO.getEmployee().getEmployeeName() : null;
            return new LoginResponse(token, employeeName, userDTO.getCustomerName(), userDTO.getCustomerId(), userDTO.getId());
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Sai tên đăng nhập hoặc mật khẩu");

        } catch (UsernameNotFoundException e) {
            throw new RuntimeException(" Không tìm thấy người dùng");

        } catch (Exception e) {
            throw new RuntimeException(" Lỗi đăng nhập: " + e.getMessage());
        }
    }

    public LoginResponse loginUserOnly(LoginRequest request) {
        LoginResponse response = login(request);
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (user.getRole() != 3) {
            throw new RuntimeException("Chỉ người dùng mới được phép đăng nhập");
        }

        return response;
    }

}