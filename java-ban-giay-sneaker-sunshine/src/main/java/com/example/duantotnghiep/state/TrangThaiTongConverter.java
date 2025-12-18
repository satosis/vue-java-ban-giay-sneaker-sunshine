package com.example.duantotnghiep.state;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class TrangThaiTongConverter implements AttributeConverter<TrangThaiTong, Integer> {
    @Override
    public Integer convertToDatabaseColumn(TrangThaiTong attribute) {
        return attribute != null ? attribute.getMa() : null;
    }

    @Override
    public TrangThaiTong convertToEntityAttribute(Integer dbData) {
        return dbData != null ? TrangThaiTong.tuMa(dbData) : null;
    }
}

