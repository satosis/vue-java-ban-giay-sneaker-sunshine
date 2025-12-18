package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.dto.request.ComplaintRequest;
import com.example.duantotnghiep.dto.response.ComplaintResponse;
import com.example.duantotnghiep.mapper.ComplaintMapper;
import com.example.duantotnghiep.model.*;
import com.example.duantotnghiep.repository.ComplaintImageRepository;
import com.example.duantotnghiep.repository.ComplaintRepository;
import com.example.duantotnghiep.repository.InvoiceRepository;
import com.example.duantotnghiep.repository.UserRepository;
import com.example.duantotnghiep.service.ComplainService;
import com.example.duantotnghiep.state.TrangThaiKhieuNai;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ComplaintServiceImpl implements ComplainService {
    private final ComplaintRepository complaintRepository;
    private final ComplaintImageRepository complaintImageRepository;
    private final ComplaintMapper complaintMapper;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;

    @Override
    public List<ComplaintResponse> getAll() {
        return List.of();
    }

    @Override
    public void addComplain(ComplaintRequest complaintRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với username: " + username));

        Customer customer = user.getCustomer();
        if (customer == null) {
            throw new RuntimeException("Người dùng không phải là nhân viên.");
        }
        Complaint complaint = complaintMapper.toEntity(complaintRequest);
        Invoice invoice = invoiceRepository.findById(complaintRequest.getInvoiceId()).get();
        complaint.setCustomer(customer);
        complaint.setInvoice(invoice);
        complaint.setCreatedDate(new Date());
        complaint.setStatus(TrangThaiKhieuNai.CHO_XU_LY);
        Complaint saved = complaintRepository.save(complaint);

        if (complaintRequest.getFiles() != null) {
            List<ComplaintImage> images = new ArrayList<>();
            for (MultipartFile file : complaintRequest.getFiles()) {
                try {
                    ComplaintImage image = new ComplaintImage();
                    image.setComplaint(saved);
                    image.setFileType(file.getContentType());
                    image.setFileName(file.getOriginalFilename());
                    image.setImageData(file.getBytes());
                    images.add(image);
                } catch (IOException e) {
                    throw new RuntimeException("Error reading image file", e);
                }
            }
            complaintImageRepository.saveAll(images);
        }
    }

    @Override
    public void updateComplain(ComplaintRequest complaintRequest) {
        Optional<Complaint> optionalComplaint = complaintRepository.findById(complaintRequest.getId());
        if (optionalComplaint.isPresent()) {
            Complaint complaint = optionalComplaint.get();
            complaint.setReason(complaintRequest.getReason());
            complaint.setStatus(complaintRequest.getStatus());
            complaint.setDescription(complaintRequest.getDescription());
            complaint.setUpdatedDate(new Date());
            complaint.setStatus(complaintRequest.getStatus());
            complaintRepository.save(complaint);

            if (complaintRequest.getFiles() != null) {
                List<ComplaintImage> images = new ArrayList<>();
                for (MultipartFile file : complaintRequest.getFiles()) {
                    try {
                        ComplaintImage image = new ComplaintImage();
                        image.setComplaint(complaint);
                        image.setFileType(file.getContentType());
                        image.setFileName(file.getOriginalFilename());
                        image.setImageData(file.getBytes());
                        images.add(image);
                    } catch (IOException e) {
                        throw new RuntimeException("Error reading image file", e);
                    }
                }
                complaintImageRepository.saveAll(images);
            }
        } else {
            throw new RuntimeException("Complaint not found with id: " + complaintRequest.getId());
        }
    }

    @Override
    public void deleteComplain(Long id) {
        complaintRepository.findById(id).ifPresent(complaint -> {
            complaint.setStatus(TrangThaiKhieuNai.DA_HUY);
            complaintRepository.save(complaint);
        });
    }
}
