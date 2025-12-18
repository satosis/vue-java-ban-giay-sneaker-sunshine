package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.dto.request.InvoiceRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TempInvoiceStore {
    private final Map<String, InvoiceRequest> store = new ConcurrentHashMap<>();

    public void put(String appTransId, InvoiceRequest request) {
        store.put(appTransId, request);
    }

    public InvoiceRequest get(String appTransId) {
        return store.get(appTransId);
    }

    public void remove(String appTransId) {
        store.remove(appTransId);
    }
}
