package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.request.VoucherRequest;
import com.example.duantotnghiep.dto.request.VoucherSearchRequest;
import com.example.duantotnghiep.dto.response.PaginationDTO;
import com.example.duantotnghiep.dto.response.VoucherResponse;
import com.example.duantotnghiep.dto.response.VoucherStatusDTO;
import com.example.duantotnghiep.mapper.VoucherMapper;
import com.example.duantotnghiep.model.Voucher;
import com.example.duantotnghiep.service.VoucherService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/vouchers")
public class VoucherController {

    private final VoucherService voucherService;
    private final VoucherMapper voucherMapper;

    public VoucherController(VoucherService voucherService, VoucherMapper voucherMapper) {
        this.voucherService = voucherService;
        this.voucherMapper = voucherMapper;
    }

    @GetMapping("/valid")
    public ResponseEntity<List<VoucherResponse>> getValidVouchers() {
        List<VoucherResponse> vouchers = voucherService.getValidVouchers();
        return ResponseEntity.ok(vouchers);
    }

    @PostMapping("/search")
    public ResponseEntity<PaginationDTO<VoucherResponse>> searchVouchers(
            @RequestBody VoucherSearchRequest request) {

        int page = (request.getPage() != null && request.getPage() >= 0) ? request.getPage() : 0;
        int size = (request.getSize() != null && request.getSize() > 0) ? request.getSize() : 5;

        Pageable pageable = PageRequest.of(page, size);
        PaginationDTO<VoucherResponse> result = voucherService.phanTrangHienThi(request, pageable);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/by-invoice/{invoiceId}")
    public ResponseEntity<?> getVouchersByInvoiceId(@PathVariable Long invoiceId) {
        try {
            List<VoucherResponse> responseList = voucherService.getVouchersByCustomerInInvoice(invoiceId);
            return ResponseEntity.ok(responseList);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<VoucherResponse> create(@RequestBody VoucherRequest request) {
        return ResponseEntity.ok(voucherService.themMoi(request));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<VoucherResponse> update(@PathVariable Long id, @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(voucherService.capNhat(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVoucher(@PathVariable Long id) {
        try {
            voucherService.deteleVoucherById(id);
            return ResponseEntity.ok("Đã vô hiệu hoá voucher và cập nhật các hoá đơn đang dùng.");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            // Voucher đã bị xoá trước đó
            return ResponseEntity.badRequest().body("Voucher này đã bị xoá trước đó.");
            // hoặc: return ResponseEntity.status(HttpStatus.CONFLICT).body("Voucher này đã bị xoá trước đó.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi vô hiệu hoá voucher");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getVoucherById(@PathVariable Long id) {
        Optional<VoucherResponse> voucherOpt = voucherService.getOne(id);
        if (voucherOpt.isPresent()) {
            return ResponseEntity.ok(voucherOpt.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy voucher với ID: " + id);
        }
    }

    @GetMapping("/apply")
    public VoucherResponse applyVoucher(
            @RequestParam Long customerId,
            @RequestParam String voucherCode,
            @RequestParam BigDecimal orderTotal
    ) {
        Voucher voucher = voucherService.validateVoucher(customerId, voucherCode, orderTotal);
        return voucherMapper.toDto(voucher);
    }

    @GetMapping("/apply-best")
    public VoucherResponse applyBestVoucher(
            @RequestParam Long customerId,
            @RequestParam BigDecimal orderTotal
    ) {
        Voucher bestVoucher = voucherService.findBestVoucherForCustomer(customerId, orderTotal);

        if (bestVoucher == null) {
            throw new RuntimeException("Không tìm thấy voucher phù hợp");
        }

        return voucherMapper.toDto(bestVoucher);
    }

    @GetMapping("/by-customer/{customerId}")
    public ResponseEntity<List<VoucherResponse>> getVouchersByCustomer(
            @PathVariable Long customerId,
            @RequestParam(required = false) Integer orderType,
            @RequestParam(required = false) Set<Long> productIds,
            @RequestParam(required = false) Set<Long> categoryIds
    ) {
        List<VoucherResponse> response = voucherService.getVouchersByCustomer(
                customerId,
                orderType,
                productIds,
                categoryIds
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get-voucher/{voucherId}")
    public ResponseEntity<?> getVouchers(@RequestParam("voucherId") Long voucherId) {
        VoucherStatusDTO response = voucherService.getVoucherStatsForToday(voucherId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/export-excel/by-ids")
    public void exportExcelByIds(@RequestBody List<Long> voucherIds, HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=vouchers-by-ids.xlsx");
            voucherService.exportVoucherToExcelByIds(voucherIds, response.getOutputStream());
            response.flushBuffer(); // OK: không close, container sẽ quản lý
        } catch (IOException e) {
            System.err.println("Xuất Excel theo IDs thất bại: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }


}

