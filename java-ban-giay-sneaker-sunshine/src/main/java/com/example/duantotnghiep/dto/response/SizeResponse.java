package com.example.duantotnghiep.dto.response;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Data
public class SizeResponse {
    Long id;
    @Size(max = 50)
    String sizeCode;
    @NotNull
    @Size(max = 250)
    String sizeName;
    Integer status;
    Date createdDate;
    Date updatedDate;
    @Size(max = 50)
    String createdBy;
    @Size(max = 50)
    String updatedBy;
}