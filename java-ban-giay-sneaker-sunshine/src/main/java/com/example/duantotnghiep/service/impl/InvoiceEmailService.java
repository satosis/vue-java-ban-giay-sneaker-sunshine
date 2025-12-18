package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.model.AddressCustomer;
import com.example.duantotnghiep.model.Invoice;
import com.example.duantotnghiep.model.InvoiceDetail;
import com.example.duantotnghiep.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class InvoiceEmailService {
        private final EmailService emailService;

        public void sendInvoiceEmail(Invoice invoice) {
            String recipientEmail = invoice.getCustomer().getEmail();
            String customerName = invoice.getCustomer().getCustomerName();
            String subject = "🧾 Hóa đơn mua hàng của bạn từ cửa hàng";

            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

            String formattedTotal = currencyFormatter.format(invoice.getTotalAmount());
            String formattedDiscount = currencyFormatter.format(invoice.getDiscountAmount());
            String formattedShipping = currencyFormatter.format(invoice.getShippingFee());
            String formattedFinal = currencyFormatter.format(invoice.getFinalAmount());
            String formattedDate = String.valueOf(invoice.getCreatedDate());

            // Địa chỉ giao hàng
            AddressCustomer address = invoice.getCustomer().getAddressList().stream()
                    .filter(AddressCustomer::getDefaultAddress)
                    .findFirst().orElse(null);

            StringBuilder content = new StringBuilder();
            content.append("<div style='font-family:Arial,sans-serif;font-size:14px;color:#333;'>")
                    .append("<h2 style='color:#2e6c80;'>Xin chào ").append(customerName).append(",</h2>")
                    .append("<p>Cảm ơn bạn đã đặt hàng tại cửa hàng của chúng tôi. Dưới đây là chi tiết hóa đơn:</p>")

                    // Thông tin hóa đơn
                    .append("<p><strong>ID đơn hàng:</strong> ").append(invoice.getInvoiceCode()).append("</p>")
                    .append("<p><strong>Ngày đặt hàng:</strong> ").append(formattedDate).append("</p>")

                    // Bảng chi tiết sản phẩm
                    .append("<h3>Tóm tắt sản phẩm</h3>")
                    .append("<table style='width:100%;border-collapse:collapse;margin:20px 0;'>")
                    .append("<tr style='background-color:#f2f2f2;'>")
                    .append("<th style='border:1px solid #ddd;padding:8px;'>Sản phẩm</th>")
                    .append("<th style='border:1px solid #ddd;padding:8px;'>Màu sắc</th>")
                    .append("<th style='border:1px solid #ddd;padding:8px;'>Kích thước</th>")
                    .append("<th style='border:1px solid #ddd;padding:8px;'>Số lượng</th>")
                    .append("<th style='border:1px solid #ddd;padding:8px;'>Đơn giá</th>")
                    .append("<th style='border:1px solid #ddd;padding:8px;'>Thành tiền</th>")
                    .append("</tr>");

            for (InvoiceDetail detail : invoice.getInvoiceDetails()) {
                String productName = detail.getProductDetail().getProduct().getProductName();
                String color = detail.getProductDetail().getColor().getColorName();
                String size = detail.getProductDetail().getSize().getSizeName();
                int quantity = detail.getQuantity();
                String unitPrice = currencyFormatter.format(detail.getProductDetail().getSellPrice());
                String lineTotal = currencyFormatter.format(
                        detail.getProductDetail().getSellPrice().multiply(BigDecimal.valueOf(quantity)));

                content.append("<tr>")
                        .append("<td style='border:1px solid #ddd;padding:8px;'>").append(productName).append("</td>")
                        .append("<td style='border:1px solid #ddd;padding:8px;'>").append(color).append("</td>")
                        .append("<td style='border:1px solid #ddd;padding:8px;'>").append(size).append("</td>")
                        .append("<td style='border:1px solid #ddd;padding:8px;text-align:center;'>").append(quantity).append("</td>")
                        .append("<td style='border:1px solid #ddd;padding:8px;text-align:right;'>").append(unitPrice).append("</td>")
                        .append("<td style='border:1px solid #ddd;padding:8px;text-align:right;'>").append(lineTotal).append("</td>")
                        .append("</tr>");
            }

            content.append("</table>")

                    // Tóm tắt đơn hàng
                    .append("<h3>Tóm tắt kiện hàng</h3>")
                    .append("<p><strong>Tổng phụ:</strong> ").append(formattedTotal).append("</p>")
                    .append("<p><strong>Vận chuyển:</strong> ").append(formattedShipping).append("</p>")
                    .append("<p><strong>Phiếu giảm giá:</strong> - ").append(formattedDiscount).append("</p>")
                    .append("<p><strong>Tổng thanh toán:</strong> <span style='color:#d9534f;font-weight:bold;'>")
                    .append(formattedFinal).append("</span></p>")

                    // Địa chỉ giao hàng
                    .append("<h3>Địa chỉ giao hàng</h3>");
            if (address != null) {
                content.append("<p>").append(customerName).append("<br>")
                        .append(invoice.getCustomer().getPhone()).append("<br>")
                        .append(address.getHouseName()).append(", ")
                        .append(address.getWardName()).append(", ")
                        .append(address.getDistrictName()).append(", ")
                        .append(address.getProvinceName()).append(", Việt Nam</p>");
            }

            content.append("<br><p>Trân trọng,</p>")
                    .append("<p><em>Đội ngũ cửa hàng</em></p>")
                    .append("</div>");

            emailService.sendEmail(recipientEmail, subject, content.toString());
        }
}
