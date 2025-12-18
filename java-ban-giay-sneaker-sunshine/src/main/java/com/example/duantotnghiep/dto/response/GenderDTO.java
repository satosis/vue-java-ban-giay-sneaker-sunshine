package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenderDTO {
    private Long id;
    private String genderName;
    private String genderCode;
    private Integer status;
}