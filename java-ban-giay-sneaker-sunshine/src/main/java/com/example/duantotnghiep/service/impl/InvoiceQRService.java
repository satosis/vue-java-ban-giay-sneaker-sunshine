package com.example.duantotnghiep.service.impl;


import com.example.duantotnghiep.dto.response.InvoiceDisplayResponse;
import com.example.duantotnghiep.mapper.InvoiceMapper;
import com.example.duantotnghiep.model.Invoice;
import com.example.duantotnghiep.model.InvoiceDetail;
import com.example.duantotnghiep.repository.InvoiceDetailRepository;
import com.example.duantotnghiep.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InvoiceQRService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceDetailRepository detailRepository;

    @Autowired
    private InvoiceMapper invoiceMapper;

    public Optional<InvoiceDisplayResponse> getInvoiceByInvoiceCode(String invoiceCode) {
        Optional<Invoice> invoiceOpt = invoiceRepository.findByInvoiceCodeQR(invoiceCode);
        if (invoiceOpt.isEmpty()) {
            return Optional.empty();
        }
        List<InvoiceDetail> details = detailRepository.findByInvoiceCodeQR(invoiceCode);
        InvoiceDisplayResponse response = invoiceMapper.toInvoiceDisplayResponse(invoiceOpt.get(), details);
        return Optional.of(response);
    }

}
