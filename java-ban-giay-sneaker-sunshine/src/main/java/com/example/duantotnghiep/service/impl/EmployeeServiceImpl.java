package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.dto.request.EmployeeRequest;
import com.example.duantotnghiep.dto.response.EmployeeResponse;
import com.example.duantotnghiep.mapper.EmployeeMapper;
import com.example.duantotnghiep.mapper.UserMapper;
import com.example.duantotnghiep.model.Employee;
import com.example.duantotnghiep.model.User;
import com.example.duantotnghiep.repository.EmployeeRepository;
import com.example.duantotnghiep.repository.UserRepository;
import com.example.duantotnghiep.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final EmployeeMapper employeeMapper;
    private final EmployeeRepository employeeRepository;
    @Override
    public Page<EmployeeResponse> getAllUsers(Pageable pageable) {
        Page<User> userPage = userRepository.findAllByEmployeeIsNotNullAndStatusIsOne(pageable);
        return userPage.map(userMapper::toEmployeeResponse);
    }

    @Override
    public EmployeeResponse getUserById(Long id) {
        User user = userRepository.findByEmployeeId(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        if (user.getEmployee() == null || user.getEmployee().getStatus() != 1) {
            throw new RuntimeException("No active Employee associated with user id: " + id);
        }
        return userMapper.toEmployeeResponse(user);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public EmployeeResponse updateUser(Long id, EmployeeRequest employeeRequest) {
        User user = userRepository.findByEmployeeId(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));

        Employee employee = user.getEmployee();

        // Kiểm tra trùng lặp username
        if (!user.getUsername().equals(employeeRequest.getUsername()) &&
                userRepository.existsByUsernameAndEmployeeStatus(employeeRequest.getUsername(),1)) {
            throw new RuntimeException("Username already exists");
        }

        // Kiểm tra trùng lặp email và phone
        if (!employee.getEmail().equals(employeeRequest.getEmail()) &&
                employeeRepository.existsByEmailAndStatus(employeeRequest.getEmail(),1)) {
            throw new RuntimeException("Email already exists");
        }
        if (!employee.getPhone().equals(employeeRequest.getPhone()) &&
                employeeRepository.existsByPhoneAndStatus(employeeRequest.getPhone(),1)) {
            throw new RuntimeException("Phone number already exists");
        }
        userMapper.updateEmployeeFromRequest(employeeRequest, employee);
        employee.setUpdatedDate(new Date());
        employee.setUpdatedBy("admin");
        employeeRepository.save(employee);

        //  Cập nhật User
        user.setUsername(employeeRequest.getUsername());
        user.setPassword(employeeRequest.getPassword());
        user.setRole(employeeRequest.getRole());
        user.setUpdatedAt(new Date());
        user.setUpdatedBy(employeeRequest.getUpdatedBy());
        userRepository.save(user);
        return userMapper.toEmployeeResponse(user);
    }


    @Override
    public void deleteUser(Long id) {
        // Kiểm tra tồn tại và status
        Optional<User> userOpt = userRepository.findByEmployeeId(id);
        if (userOpt.isEmpty() || userOpt.get().getEmployee().getStatus() != 1) {
            throw new RuntimeException("Không tìm thấy nhân viên đang hoạt động với ID: " + id);
        }

        // Gọi repository để update status = 0
        userRepository.deleteByEmployeeId(id);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public EmployeeResponse createUser(EmployeeRequest employeeRequest) {
        if (userRepository.existsByUsernameAndEmployeeStatus(employeeRequest.getUsername(),1)) {
            throw new RuntimeException("Username already exists");
        }

        if (employeeRepository.existsByEmailAndStatus(employeeRequest.getEmail(),1)) {
            throw new RuntimeException("Email already exists");
        }

        if (employeeRepository.existsByPhoneAndStatus(employeeRequest.getPhone(),1)) {
            throw new RuntimeException("Phone number already exists");
        }

        Employee employee = userMapper.toEmployeeEntity(employeeRequest);
        employee.setEmployeeCode(generateEmployeeCode());
        employee.setCreatedDate(new Date());
        employee.setCreatedBy("admin");
        employee.setStatus(1); // Mặc định status = 1
        employee = employeeRepository.save(employee);

        User user = userMapper.toUserEntity(employeeRequest);
        user.setEmployee(employee);
        user.setPassword(employeeRequest.getPassword());
        user.setCreatedAt(new Date());
        user.setCreatedBy("admin");
        user = userRepository.save(user);

        return userMapper.toEmployeeResponse(user);
    }

    @Override
    public List<EmployeeResponse> getAllData() {
        List<Employee> employees = employeeRepository.getData();
        return employeeMapper.toEmployeeResponseList(employees);
    }

    private String generateEmployeeCode() {
        String prefix = "EMPLOYEE-";
        String datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String randomPart = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + datePart + "-" + randomPart;
    }

    @Transactional(readOnly = true)
    @Override
    public List<EmployeeResponse> searchEmployees(String employeeCode, String employeeName, String email) {
        List<Employee> employees = employeeRepository.searchEmployees(employeeCode, employeeName, email);
        return employeeMapper.toEmployeeResponseList(employees);
    }
}
