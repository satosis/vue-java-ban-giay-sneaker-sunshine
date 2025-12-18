package com.example.duantotnghiep.state;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class TrangThaiKhieuNaiConverter implements AttributeConverter<TrangThaiKhieuNai, Integer> {
    @Override
    public Integer convertToDatabaseColumn(TrangThaiKhieuNai attribute) {
        return attribute != null ? attribute.getMa() : null;
    }

    @Override
    public TrangThaiKhieuNai convertToEntityAttribute(Integer dbData) {
        return dbData != null ? TrangThaiKhieuNai.tuMa(dbData) : null;
    }
}
