package com.sagarpandey.activity_tracker.Mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sagarpandey.activity_tracker.models.ScheduleSpec;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ScheduleSpecConverter implements AttributeConverter<ScheduleSpec, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(ScheduleSpec attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting ScheduleSpec to JSON", e);
        }
    }

    @Override
    public ScheduleSpec convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, ScheduleSpec.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error reading ScheduleSpec from JSON data", e);
        }
    }
}
