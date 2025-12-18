package com.example.duantotnghiep.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductImageResponse {
    private Long id;
    private String imageName;
    private String status;
    private Long colorId;
    private String colorName;
    private byte[] image;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date createdDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date modifiedDate;
    private String createdBy;
    private String modifiedBy;

    public ProductImageResponse(Long id, String imageName, byte[] image,String colorName) {
        this.id = id;
        this.imageName = imageName;
        this.image = image;
        this.colorName = colorName;
    }

}
