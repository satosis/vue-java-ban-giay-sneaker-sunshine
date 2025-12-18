package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.request.InvoiceQRCodeRequest;
import com.example.duantotnghiep.dto.response.InvoiceDisplayResponse;
import com.example.duantotnghiep.dto.response.InvoiceResponse;
import com.example.duantotnghiep.model.Invoice;
import com.example.duantotnghiep.model.InvoiceDetail;
import com.example.duantotnghiep.repository.InvoiceDetailRepository;
import com.example.duantotnghiep.service.InvoiceService;
import com.example.duantotnghiep.service.impl.ExcelExportService;
import com.example.duantotnghiep.service.impl.InvoiceExportService;
import com.example.duantotnghiep.service.impl.InvoiceExportServiceOnline;
import com.example.duantotnghiep.service.impl.InvoiceQRService;
import com.example.duantotnghiep.state.TrangThaiChiTiet;
import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceExportService invoiceExportService;
    private final InvoiceDetailRepository invoiceDetailRepository;
    private final ExcelExportService excelExportService;
    private final InvoiceQRService invoiceQRService;
    private final InvoiceExportServiceOnline invoiceExportServiceOnline;

    @GetMapping
    public ResponseEntity<Page<InvoiceDisplayResponse>> getInvoiceDisplays(
            @PageableDefault(sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<InvoiceDisplayResponse> page = invoiceService.getInvoiceDisplays(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}/detail")
    public InvoiceDisplayResponse getInvoiceWithDetails(@PathVariable Long id) {
        return invoiceService.getInvoiceWithDetails(id);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<InvoiceResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String counterStatusKey,  // trạng thái TẠI QUẦY
            @RequestParam(required = false) String onlineStatusKey,   // trạng thái ONLINE
            @RequestParam(required = false) String createdDate,       // yyyy-MM-dd hoặc ISO
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        LocalDate parsedDate = null;
        if (createdDate != null) {
            String cd = createdDate.trim();
            if (!cd.isEmpty() && !"null".equalsIgnoreCase(cd) && !"undefined".equalsIgnoreCase(cd)) {
                if (cd.matches("^\\d{4}-\\d{2}-\\d{2}.*$")) cd = cd.substring(0, 10); // hỗ trợ ISO
                parsedDate = LocalDate.parse(cd);
            }
        }

        return ResponseEntity.ok(
                invoiceService.searchSeparatedStatus(
                        keyword, counterStatusKey, onlineStatusKey, parsedDate, pageable
                )
        );
    }

    @GetMapping("/{invoiceCode}/export")
    public void exportInvoice(
            @PathVariable String invoiceCode,
            HttpServletResponse response) throws IOException, DocumentException {

        Invoice invoice = invoiceService.findByInvoiceCode(invoiceCode);
        if (invoice == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invoice not found");
            return;
        }

        List<InvoiceDetail> details = invoiceDetailRepository.findByInvoiceId(invoice.getId());
        invoiceExportService.exportInvoice(response, invoice, details);
    }

    @GetMapping("/{id}/export-online")
    public void exportInvoiceOnlineById(
            @PathVariable Long id,
            HttpServletResponse response) throws IOException, DocumentException {

        // Lấy invoice theo id
        Invoice invoice = invoiceService.findById(id);
        if (invoice == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invoice not found");
            return;
        }

        // Chỉ cho phép in nếu là hóa đơn online và ở trạng thái ĐÃ XỬ LÝ
        if (invoice.getOrderType() == 1
                && invoice.getStatusDetail() != TrangThaiChiTiet.DA_XU_LY) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Chỉ hóa đơn online ở trạng thái ĐÃ XỬ LÝ mới được in");
            return;
        }

        // Lấy chi tiết hóa đơn
        List<InvoiceDetail> details = invoiceDetailRepository.findByInvoiceId(invoice.getId());

        // Gọi service in hóa đơn online
        invoiceExportServiceOnline.exportInvoiceOnline(response, invoice, details);
    }

    @GetMapping("/{id}/export-id")
    public void exportInvoiceById(
            @PathVariable Long id,
            HttpServletResponse response) throws IOException, DocumentException {

        Invoice invoice = invoiceService.findById(id); // Giả sử bạn có phương thức này trong service
        if (invoice == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invoice not found");
            return;
        }

        List<InvoiceDetail> details = invoiceDetailRepository.findByInvoiceId(invoice.getId());
        invoiceExportService.exportInvoice(response, invoice, details);
    }

    @GetMapping("/export-excel")
    public void exportInvoicesToExcel(
            @RequestParam(value = "invoiceIds", required = false) List<Long> invoiceIds,
            HttpServletResponse response) throws IOException {

        // Lấy danh sách hóa đơn theo ID hoặc tất cả nếu không truyền
        List<InvoiceDisplayResponse> invoices = (invoiceIds != null && !invoiceIds.isEmpty())
                ? invoiceService.getInvoicesWithDetailsByIds(invoiceIds)
                : invoiceService.getAllInvoicesWithDetails();

        // Nếu không có dữ liệu, trả lỗi 404
        if (invoices == null || invoices.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy hóa đơn nào phù hợp");
            return;
        }

        // Thiết lập header cho file Excel trả về
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"hoa_don.xlsx\"");

        // Ghi file Excel ra response output stream
        try (ByteArrayInputStream excelStream = excelExportService.exportInvoicesToExcel(invoices)) {
            IOUtils.copy(excelStream, response.getOutputStream());
            response.flushBuffer();
        }
    }

    @PostMapping("/qr-scan")
    public ResponseEntity<Object> scanQRCode(@RequestBody InvoiceQRCodeRequest request) {
        String invoiceCode = request.getInvoiceCode();
        if (invoiceCode == null || invoiceCode.isBlank()) {
            return ResponseEntity.badRequest().body("Mã hóa đơn (invoiceCode) không được để trống");
        }

        return invoiceQRService.getInvoiceByInvoiceCode(invoiceCode)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body("Không tìm thấy hóa đơn từ mã QR"));
    }

}
