package com.example.duantotnghiep.state;


import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class TrangThaiChiTietConverter implements AttributeConverter<TrangThaiChiTiet, Integer> {
    @Override
    public Integer convertToDatabaseColumn(TrangThaiChiTiet attribute) {
        return attribute != null ? attribute.getMa() : null;
    }

    @Override
    public TrangThaiChiTiet convertToEntityAttribute(Integer dbData) {
        return dbData != null ? TrangThaiChiTiet.tuMa(dbData) : null;
    }
}
