// InvoiceEmailServiceStatus.java
package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.model.AddressCustomer;
import com.example.duantotnghiep.model.Invoice;
import com.example.duantotnghiep.model.InvoiceDetail;
import com.example.duantotnghiep.service.EmailService;
import com.example.duantotnghiep.state.TrangThaiChiTiet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;   // ✅ dùng SimpleDateFormat cho java.util.Date
import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InvoiceEmailServiceStatus {

    private final EmailService emailService;

    private static final NumberFormat VND = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private static final SimpleDateFormat DATE_TIME = new SimpleDateFormat("dd/MM/yyyy HH:mm"); // ✅

    public void sendStatusEmail(Invoice invoice, TrangThaiChiTiet status) {
        if (invoice == null || invoice.getCustomer() == null) return;

        String recipientEmail = safe(invoice.getCustomer().getEmail());
        if (recipientEmail.isBlank()) return;

        String customerName = safe(invoice.getCustomer().getCustomerName());
        String subject = buildSubject(status, invoice.getInvoiceCode());
        String html = buildInvoiceHtml(invoice, customerName, status);

        emailService.sendEmail(recipientEmail, subject, html);
    }

    private String buildSubject(TrangThaiChiTiet status, String invoiceCode) {
        switch (status) {
            case DANG_GIAO_HANG:
                return "🚚 Đơn " + safe(invoiceCode) + " đang được giao tới bạn";
            case GIAO_THANH_CONG:
                return "✅ Đơn " + safe(invoiceCode) + " đã giao hàng thành công";
            default:
                return "🧾 Cập nhật đơn " + safe(invoiceCode);
        }
    }

    private String buildInvoiceHtml(Invoice invoice, String customerName, TrangThaiChiTiet status) {
        String formattedTotal    = VND.format(nz(invoice.getTotalAmount()));
        String formattedDiscount = VND.format(nz(invoice.getDiscountAmount()));
        String formattedShipping = VND.format(nz(invoice.getShippingFee()));
        String formattedFinal    = VND.format(nz(invoice.getFinalAmount()));
        // ✅ format Date đúng cách
        String formattedDate     = (invoice.getCreatedDate() == null) ? "" : DATE_TIME.format(invoice.getCreatedDate());

        AddressCustomer address = Optional.ofNullable(invoice.getCustomer().getAddressList())
                .flatMap(list -> list.stream().filter(AddressCustomer::getDefaultAddress).findFirst())
                .orElse(null);

        String statusText  = status.getMoTa();
        String statusColor = (status == TrangThaiChiTiet.DANG_GIAO_HANG) ? "#1e88e5"
                : (status == TrangThaiChiTiet.GIAO_THANH_CONG) ? "#2e7d32"
                : "#6c757d";

        StringBuilder content = new StringBuilder();
        content.append("<div style='font-family:Arial,sans-serif;font-size:14px;color:#333;'>")
                .append("<h2 style='color:#2e6c80;margin:0 0 8px 0'>Xin chào ").append(escape(customerName)).append(",</h2>")
                .append("<p style='margin:0 0 16px 0'>Đơn hàng của bạn vừa được cập nhật trạng thái:</p>")
                .append("<div style='display:inline-block;padding:6px 10px;border-radius:999px;background:")
                .append(statusColor).append(";color:#fff;font-weight:600'>")
                .append(escape(statusText))
                .append("</div>")
                .append("<hr style='margin:16px 0;border:none;border-top:1px solid #eee'/>")
                .append("<p><strong>Mã đơn:</strong> ").append(escape(safe(invoice.getInvoiceCode()))).append("</p>")
                .append("<p><strong>Ngày tạo:</strong> ").append(escape(formattedDate)).append("</p>");

        // (tuỳ bạn) Nếu có trackingCode/trackingUrl thì hiển thị ở đây
        // if (invoice.getTrackingCode() != null && !invoice.getTrackingCode().isBlank()) { ... }

        // Bảng chi tiết
        content.append("<h3 style='margin-top:20px'>Tóm tắt sản phẩm</h3>")
                .append("<table style='width:100%;border-collapse:collapse;margin:10px 0;'>")
                .append("<thead><tr style='background:#f6f7f9'>")
                .append(th("Sản phẩm")).append(th("Màu sắc")).append(th("Kích thước"))
                .append(th("Số lượng")).append(th("Đơn giá")).append(th("Thành tiền"))
                .append("</tr></thead><tbody>");

        for (InvoiceDetail d : invoice.getInvoiceDetails()) {
            String productName = safe(d.getProductDetail().getProduct().getProductName());
            String color       = safe(d.getProductDetail().getColor().getColorName());
            String size        = safe(d.getProductDetail().getSize().getSizeName());
            int qty            = d.getQuantity();
            BigDecimal sell    = nz(d.getProductDetail().getSellPrice());
            String unitPrice   = VND.format(sell);
            String lineTotal   = VND.format(sell.multiply(BigDecimal.valueOf(qty)));

            content.append("<tr>")
                    .append(td(productName))
                    .append(td(color))
                    .append(td(size))
                    .append(tdCenter(String.valueOf(qty)))
                    .append(tdRight(unitPrice))
                    .append(tdRight(lineTotal))
                    .append("</tr>");
        }
        content.append("</tbody></table>");

        content.append("<h3 style='margin-top:18px'>Tóm tắt kiện hàng</h3>")
                .append(pMoney("Tổng phụ", formattedTotal))
                .append(pMoney("Vận chuyển", formattedShipping))
                .append(pMoney("Phiếu giảm giá", "- " + formattedDiscount))
                .append("<p><strong>Tổng thanh toán:</strong> <span style='color:#d9534f;font-weight:700'>")
                .append(formattedFinal).append("</span></p>");

        content.append("<h3 style='margin-top:18px'>Địa chỉ giao hàng</h3>");
        if (address != null) {
            content.append("<p>")
                    .append(escape(customerName)).append("<br>")
                    .append(escape(safe(invoice.getCustomer().getPhone()))).append("<br>")
                    .append(escape(safe(address.getHouseName()))).append(", ")
                    .append(escape(safe(address.getWardName()))).append(", ")
                    .append(escape(safe(address.getDistrictName()))).append(", ")
                    .append(escape(safe(address.getProvinceName()))).append(", Việt Nam")
                    .append("</p>");
        }

        content.append("<br><p>Trân trọng,<br/><em>Đội ngũ cửa hàng</em></p>")
                .append("</div>");

        return content.toString();
    }

    // helpers
    private static String safe(String s) { return s == null ? "" : s; }
    private static BigDecimal nz(BigDecimal b) { return b == null ? BigDecimal.ZERO : b; }
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
    private static String th(String text) {
        return "<th style='border:1px solid #e7e7e7;padding:8px;text-align:left;font-weight:600'>" + escape(text) + "</th>";
    }
    private static String td(String text) {
        return "<td style='border:1px solid #eee;padding:8px;vertical-align:top'>" + escape(text) + "</td>";
    }
    private static String tdRight(String text) {
        return "<td style='border:1px solid #eee;padding:8px;text-align:right;vertical-align:top'>" + escape(text) + "</td>";
    }
    private static String tdCenter(String text) {
        return "<td style='border:1px solid #eee;padding:8px;text-align:center;vertical-align:top'>" + escape(text) + "</td>";
    }
    private static String pMoney(String label, String value) {
        return "<p><strong>" + escape(label) + ":</strong> " + escape(value) + "</p>";
    }
}
