package com.pgsa.trailers.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Component
public class JsonConverter implements AttributeConverter<Map<String, Object>, Object> { // FIX: Specify types

    private static ObjectMapper objectMapper;

    @Autowired
    public void setObjectMapper(ObjectMapper mapper) {
        JsonConverter.objectMapper = mapper;
    }

    @Override
    public Object convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(attribute);

            PGobject pgObject = new PGobject();
            pgObject.setType("json");
            pgObject.setValue(json);
            return pgObject;

        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting to JSON", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(Object dbData) { // FIX: Return Map type
        if (dbData == null) {
            return new HashMap<>();
        }

        try {
            String json;

            if (dbData instanceof PGobject) {
                json = ((PGobject) dbData).getValue();
            } else if (dbData instanceof String) {
                json = (String) dbData;
            } else {
                throw new IllegalArgumentException("Unsupported database type: " + dbData.getClass());
            }

            if (json == null || json.isEmpty()) {
                return new HashMap<>();
            }

            // FIX: Use TypeReference to deserialize as Map<String, Object>
            return objectMapper.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting JSON to object", e);
        }
    }
}