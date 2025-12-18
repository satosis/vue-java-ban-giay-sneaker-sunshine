package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.model.Invoice;
import com.example.duantotnghiep.model.InvoiceDetail;
import com.example.duantotnghiep.model.ProductDetail;
import com.example.duantotnghiep.repository.InvoiceDetailRepository;
import com.example.duantotnghiep.repository.InvoiceRepository;
import com.example.duantotnghiep.repository.ProductDetailRepository;
import com.example.duantotnghiep.state.TrangThaiTong;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InvoiceCleanupTask {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceDetailRepository invoiceDetailRepository;
    private final ProductDetailRepository productDetailRepository;

    /**
     * Chạy mỗi 60 giây để kiểm tra hóa đơn chưa thanh toán quá 1 ngày
     */
    @Scheduled(fixedRate = 60000) // 60 giây một lần
    @Transactional
    public void cancelUnpaidInvoices() {
        // thay minusMinutes(10) -> minusDays(1)
        LocalDateTime limitTime = LocalDateTime.now().minusDays(1);

        // Hóa đơn trạng thái đang xử lý (chưa thanh toán), tạo trước hơn 1 ngày
        List<Invoice> expiredInvoices =
                invoiceRepository.findByStatusAndOrderTypeAndCreatedDateBefore(
                        TrangThaiTong.DANG_XU_LY, 0, limitTime);

        for (Invoice invoice : expiredInvoices) {
            List<InvoiceDetail> details = invoiceDetailRepository.findByInvoiceAndStatus(invoice, 1);

            // Trả tồn kho
            for (InvoiceDetail detail : details) {
                ProductDetail pd = detail.getProductDetail();
                pd.setQuantity(pd.getQuantity() + detail.getQuantity());
                productDetailRepository.save(pd);
            }

            // Xóa chi tiết
            invoiceDetailRepository.deleteAll(details);

            // Cập nhật trạng thái hóa đơn (huỷ) + về 0
            invoice.setStatus(TrangThaiTong.DA_HUY);
            invoice.setUpdatedDate(new Date());
            invoice.setTotalAmount(BigDecimal.ZERO);
            invoice.setFinalAmount(BigDecimal.ZERO);
            invoice.setDiscountAmount(BigDecimal.ZERO);
            invoiceRepository.save(invoice);
        }

        if (!expiredInvoices.isEmpty()) {
            System.out.println("Đã huỷ " + expiredInvoices.size() + " hóa đơn quá 1 ngày.");
        }
    }
}

