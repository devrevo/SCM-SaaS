package com.scm_saas.SCM_SaaS.util;

import org.bson.Document;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Arrays;

@Service
public class MongoHelper {
    private static final String[] DATE_FORMATS = {
            "yyyy-MM-dd",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy/MM/dd"
    };

    public Object mapDocumentToEntity(Document document, Class<?> entityClass) {
        try {
            Object entity = entityClass.getDeclaredConstructor().newInstance();

            for (Field field : entityClass.getDeclaredFields()) {
                field.setAccessible(true);
                String fieldName = field.getName();
                Object value = document.get(fieldName);

                if (value != null) {
                    try {
                        Object convertedValue = convertToFieldType(value, field.getType());
                        field.set(entity, convertedValue);
                        System.out.println("Successfully mapped field: " + fieldName + " with value: " + value);
                    } catch (Exception e) {
                        System.err.println("Failed to map field: " + fieldName + " - " + e.getMessage());
                    }
                }
            }

            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map document to entity", e);
        }
    }

    private Object convertToFieldType(Object value, Class<?> targetType) {
        if (value == null) return null;

        try {
            if (targetType.equals(Date.class)) {
                if (value instanceof Date) return value;
                if (value instanceof String) return parseDate((String) value);
            }

            if (targetType.equals(String.class)) return value.toString();
            if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
                return (value instanceof Number) ? ((Number) value).intValue() : Integer.parseInt(value.toString());
            }
            if (targetType.equals(Double.class) || targetType.equals(double.class)) {
                return (value instanceof Number) ? ((Number) value).doubleValue() : Double.parseDouble(value.toString());
            }
            if (targetType.equals(Boolean.class) || targetType.equals(boolean.class)) {
                return (value instanceof Boolean) ? value : Boolean.parseBoolean(value.toString());
            }

            return value;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert value: " + value + " to type: " + targetType.getName(), e);
        }
    }

    private Date parseDate(String dateStr) {
        for (String format : DATE_FORMATS) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat(format);
                return dateFormat.parse(dateStr);
            } catch (ParseException ignored) {
                // Try next format
            }
        }
        throw new RuntimeException("Unable to parse date: " + dateStr);
    }
}