package com.example.duantotnghiep.dto.response;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ColorResponse {
    Long id;
    @NotNull
    @Size(max = 50)
    String colorCode;
    @NotNull
    @Size(max = 100)
    String colorName;
    Integer status;
    Date createdDate;
    Date updatedDate;
    @Size(max = 50)
    String createdBy;
    @Size(max = 50)
    String updatedBy;
}