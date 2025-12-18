package com.example.duantotnghiep.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SupplierRequest {
    @Size(max = 250)
    @NotNull
    private String supplierName;

    @Size(max = 50)
    @NotNull
    private String country;

    @Size(max = 100)
    @NotNull
    private String province;

    @Size(max = 100)
    @NotNull
    private String district;

    @Size(max = 100)
    @NotNull
    private String ward;

    @Size(max = 250)
    @NotNull
    private String houseName;
}
