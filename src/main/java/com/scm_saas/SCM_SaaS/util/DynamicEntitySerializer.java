package com.scm_saas.SCM_SaaS.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import javassist.Modifier;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class DynamicEntitySerializer extends JsonSerializer<Object> {
    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        // Create a brand-new visited set for the entire object graph
        Set<Object> visited = new HashSet<>();
        writeDynamicEntity(value, gen, visited);
    }

    private void writeDynamicEntity(Object value, JsonGenerator gen, Set<Object> visited)
            throws IOException {

        // Null or not a dynamic entity => fallback to normal Jackson
        if (value == null) {
            gen.writeNull();
            return;
        }
        if (!value.getClass().getName().startsWith("com.scm_saas.entity")) {
            // Let Jackson handle normal String, Date, etc.
            gen.writeObject(value);
            return;
        }

        // If we’ve already visited this exact object, write a placeholder
        if (!visited.add(value)) {
            gen.writeString("[Circular Reference to " + value.getClass().getSimpleName() + "]");
            return;
        }

        // Reflect on fields
        gen.writeStartObject();

        Field[] fields = value.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);

            Object fieldValue;
            try {
                fieldValue = field.get(value);
            } catch (IllegalAccessException e) {
                fieldValue = null;
            }
            gen.writeFieldName(field.getName());
            writeDynamicEntity(fieldValue, gen, visited);
        }
        gen.writeEndObject();
    }
}
