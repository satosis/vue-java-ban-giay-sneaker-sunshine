package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InvoiceDisplayResponse {
    private InvoiceResponse invoice;
    private List<InvoiceDetailResponse> details;
}
