package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.model.Invoice;
import com.example.duantotnghiep.model.InvoiceDetail;
import com.example.duantotnghiep.qr.NumberToVietnameseWords;
import com.example.duantotnghiep.qr.QRCodeUtil;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class InvoiceExportServiceOnline {

    /**
     * In hóa đơn theo layout giống ảnh mẫu.
     */
    public void exportInvoiceOnline(HttpServletResponse resp, Invoice invoice, List<InvoiceDetail> details)
            throws IOException, DocumentException {

        // ===== HTTP headers =====
        resp.setContentType("application/pdf");
        resp.setHeader(
                "Content-Disposition",
                "attachment; filename=HD_" + safe(invoice.getInvoiceCode(), "UNKNOWN") + ".pdf"
        );

        // ===== Page setup (A5 dọc – giống khổ trong ảnh) =====
        Rectangle pageSize = PageSize.A5; // 420 x 595 pt
        Document doc = new Document(pageSize, 36, 36, 28, 36);
        PdfWriter.getInstance(doc, resp.getOutputStream());
        doc.open();

        // ===== Font Unicode =====
        BaseFont bf = loadVnBaseFont();
        Font fTitle  = new Font(bf, 18, Font.BOLD);
        Font fH1     = new Font(bf, 12, Font.BOLD);
        Font fBold   = new Font(bf, 10, Font.BOLD);
        Font fText   = new Font(bf, 10);
        Font fSmall  = new Font(bf, 9);

        // ===== Header: Store =====
        Paragraph store = new Paragraph("SunShine Sneaker Store", fTitle);
        store.setAlignment(Element.ALIGN_CENTER);
        doc.add(store);

        Paragraph storeAddr = new Paragraph("25/106/15 Phú Minh Bắc Từ Liêm Hà Nội", fText);
        storeAddr.setAlignment(Element.ALIGN_CENTER);
        doc.add(storeAddr);

        Paragraph storePhone = new Paragraph("SĐT: 0346771322 ", fText);
        storePhone.setAlignment(Element.ALIGN_CENTER);
        doc.add(storePhone);

        doc.add(new Paragraph(" ", fText));

        // ===== Invoice meta =====
        PdfPTable meta = new PdfPTable(2);
        meta.setWidths(new float[]{60, 140});
        meta.setWidthPercentage(100);

        addMeta(meta, "Mã hóa đơn:", safe(invoice.getInvoiceCode(), ""), fText, fBold);
        addMeta(meta, "Ngày tạo:", formatDate(invoice.getCreatedDate()), fText, fBold);
        addMeta(meta, "Khách hàng:", invoice.getCustomer() != null ?
                safe(invoice.getCustomer().getCustomerName(), "Khách lẻ") : "Khách lẻ", fText, fBold);
        addMeta(meta, "Số điện thoại:", invoice.getCustomer() != null ?
                safe(invoice.getCustomer().getPhone(), "Không có") : "Không có", fText, fBold);

        addMeta(meta, "Địa chỉ giao hàng:", safe(invoice.getDeliveryAddress(), "---"), fText, fBold);

        doc.add(meta);

        // ===== Section title =====
        doc.add(space(6));
        Paragraph secTitle = new Paragraph("Nội dung đơn hàng ( Tổng số lượng sản phẩm "
                + totalQty(details) + " )", fH1);
        doc.add(secTitle);
        doc.add(line(bf));

        // ===== Bảng sản phẩm =====
        PdfPTable tbl = new PdfPTable(6);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[]{7, 35, 14, 10, 15, 19}); // STT, Tên, Giá, SL, Trạng thái, Tổng

        // Header
        addHeader(tbl, "Stt", fBold);
        addHeader(tbl, "Tên", fBold);
        addHeader(tbl, "Giá", fBold, Element.ALIGN_RIGHT);
        addHeader(tbl, "Số", fBold, Element.ALIGN_CENTER);
        addHeader(tbl, "Trạng thái", fBold, Element.ALIGN_CENTER);
        addHeader(tbl, "Tổng", fBold, Element.ALIGN_RIGHT);

        // Rows
        int i = 1;
        for (InvoiceDetail d : details) {
            String name = safe(() -> d.getProductDetail().getProduct().getProductName(), "Sản phẩm");
            int qty     = d.getQuantity() == null ? 0 : d.getQuantity();

            // Hiển thị "giá hiển thị" theo rule: nếu có discountedPrice > 0 thì dùng, nếu không dùng sellPrice
            BigDecimal unitPrice = nz(d.getDiscountedPrice(), nz(d.getSellPrice(), BigDecimal.ZERO));
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));

            addCell(tbl, String.valueOf(i++), fText, Element.ALIGN_CENTER);
            addCell(tbl, name, fText, Element.ALIGN_LEFT);
            addCell(tbl, formatCurrency(unitPrice), fText, Element.ALIGN_RIGHT);
            addCell(tbl, String.valueOf(qty), fText, Element.ALIGN_CENTER);
            addCell(tbl, "Thành công", fText, Element.ALIGN_CENTER); // hoặc map trạng thái dòng nếu bạn có
            addCell(tbl, formatCurrency(lineTotal), fText, Element.ALIGN_RIGHT);
        }

        doc.add(tbl);
        doc.add(line(bf));

        // ===== Tính tổng =====
        BigDecimal totalAmount   = nz(invoice.getTotalAmount(), BigDecimal.ZERO);     // tổng gốc (chưa giảm)
        BigDecimal discount      = nz(invoice.getDiscountAmount(), BigDecimal.ZERO);  // tổng giảm (sp + voucher)
        BigDecimal shippingFee   = nz(invoice.getShippingFee(), BigDecimal.ZERO);
        BigDecimal finalAmount   = nz(invoice.getFinalAmount(), BigDecimal.ZERO);
        int totalItemQty         = totalQty(details);

        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(60);
        totals.setHorizontalAlignment(Element.ALIGN_LEFT);
        totals.setWidths(new float[]{55, 45});

        addKV(totals, "Tổng tiền hàng:", formatCurrency(totalAmount), fBold, fBold);
        addKV(totals, "Giảm giá:", "-" + formatCurrency(discount), fBold, fBold);
        addKV(totals, "Phí ship:", formatCurrency(shippingFee), fBold, fBold);
        addKV(totals, "Tổng hóa đơn:", formatCurrency(finalAmount), fH1, fH1);
        addKV(totals, "Tổng số lượng sản phẩm:", String.valueOf(totalItemQty), fBold, fBold);

        doc.add(totals);
        doc.add(space(6));

        // ===== Bằng chữ =====
        Paragraph byWords = new Paragraph(
                "Bằng chữ: " + NumberToVietnameseWords.convert(finalAmount) + " đồng.",
                fText
        );
        doc.add(byWords);
        doc.add(space(8));

        // ===== QR code (mã hóa đơn) =====
        Paragraph qrLabel = new Paragraph("Mã QR:", fText);
        doc.add(qrLabel);
        Image qr = QRCodeUtil.generateQRCode(safe(invoice.getInvoiceCode(), ""), 90, 90);
        if (qr != null) {
            qr.setAlignment(Element.ALIGN_LEFT);
            doc.add(qr);
        }

        doc.add(space(10));
        Paragraph thanks = new Paragraph("Xin cảm ơn Quý khách! Hẹn gặp lại!", fText);
        thanks.setAlignment(Element.ALIGN_CENTER);
        doc.add(thanks);

        doc.close();
    }

    // ===== Helpers =====

    private BaseFont loadVnBaseFont() throws IOException, DocumentException {
        // Ưu tiên font Việt hoá bạn đã để trong resources/fonts
        InputStream is = getClass().getResourceAsStream("/fonts/BeVietnamPro-BoldItalic.ttf");
        if (is == null) throw new FileNotFoundException("Không tìm thấy font tại /fonts/BeVietnamPro-BoldItalic.ttf");
        byte[] bytes = is.readAllBytes();
        return BaseFont.createFont(
                "BeVietnamPro-BoldItalic.ttf",
                BaseFont.IDENTITY_H,
                BaseFont.EMBEDDED,
                BaseFont.CACHED,
                bytes, null
        );
    }

    private Paragraph line(BaseFont bf) {
        // đường kẻ mảnh
        Paragraph p = new Paragraph("----------------------------------------------", new Font(bf, 9));
        p.setSpacingBefore(2f);
        p.setSpacingAfter(4f);
        return p;
    }

    private Paragraph space(float pt) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(pt);
        return p;
    }

    private void addMeta(PdfPTable t, String k, String v, Font fText, Font fBold) {
        PdfPCell c1 = new PdfPCell(new Phrase(k, fBold));
        c1.setBorder(Rectangle.NO_BORDER);
        t.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(v, fText));
        c2.setBorder(Rectangle.NO_BORDER);
        t.addCell(c2);
    }

    private void addHeader(PdfPTable t, String text, Font f) {
        addHeader(t, text, f, Element.ALIGN_LEFT);
    }

    private void addHeader(PdfPTable t, String text, Font f, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(6f);
        c.setBackgroundColor(new Color(245, 245, 245));
        t.addCell(c);
    }

    private void addCell(PdfPTable t, String text, Font f, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(5f);
        t.addCell(c);
    }

    private void addKV(PdfPTable t, String k, String v, Font fk, Font fv) {
        PdfPCell c1 = new PdfPCell(new Phrase(k, fk));
        c1.setBorder(Rectangle.NO_BORDER);
        t.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(v, fv));
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setBorder(Rectangle.NO_BORDER);
        t.addCell(c2);
    }

    private String formatCurrency(BigDecimal n) {
        if (n == null) return "0 ₫";
        return String.format("%,d ₫", n.longValue());
    }

    private String formatDate(Date d) {
        if (d == null) return "";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(d);
    }

    private BigDecimal nz(BigDecimal v, BigDecimal alt) {
        return v == null ? alt : v;
    }

    private int totalQty(List<InvoiceDetail> details) {
        int s = 0;
        if (details != null) {
            for (InvoiceDetail d : details) {
                s += (d.getQuantity() == null ? 0 : d.getQuantity());
            }
        }
        return s;
    }

    private String safe(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }

    private String safe(java.util.concurrent.Callable<String> getter, String def) {
        try {
            String v = getter.call();
            return safe(v, def);
        } catch (Exception e) {
            return def;
        }
    }
}
