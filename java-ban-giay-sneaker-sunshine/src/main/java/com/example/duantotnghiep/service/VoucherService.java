package com.example.duantotnghiep.service;

import com.example.duantotnghiep.dto.request.VoucherRequest;
import com.example.duantotnghiep.dto.request.VoucherSearchRequest;
import com.example.duantotnghiep.dto.response.PaginationDTO;
import com.example.duantotnghiep.dto.response.VoucherResponse;
import com.example.duantotnghiep.dto.response.VoucherStatusDTO;
import com.example.duantotnghiep.model.Voucher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface VoucherService {
    List<VoucherResponse> getValidVouchers();

    List<VoucherResponse> getVouchersByCustomerInInvoice(Long invoiceId);

    VoucherResponse themMoi(VoucherRequest voucherRequest);
    VoucherResponse capNhat(Long id,VoucherRequest voucherRequest);
    Optional<VoucherResponse> getOne(Long id);
    void deteleVoucherById(Long id);

    PaginationDTO<VoucherResponse> phanTrangHienThi(VoucherSearchRequest request, Pageable pageable);

    Voucher validateVoucher(Long customerId, String voucherCode, BigDecimal orderTotal);
    Voucher validateVoucherV2(Long customerId, String voucherCode, BigDecimal orderTotal);

    Voucher findBestVoucherForCustomer(Long customerId, BigDecimal orderTotal);


    @Transactional(readOnly = true)
    List<VoucherResponse> getVouchersByCustomer(
            Long customerId,
            Integer orderType,                // 0: quầy, 1: online, null: không lọc
            Set<Long> productIds,             // có thể null/rỗng
            Set<Long> categoryIds             // có thể null/rỗng
    );

    void exportVoucherToExcelByIds(List<Long> voucherIds, OutputStream outputStream) throws IOException;
    VoucherStatusDTO getVoucherStatsForToday(Long voucherId);

    Integer getStatus(String code);
}
