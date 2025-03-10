package com.scm_saas.SCM_SaaS.service;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.ValidationOptions;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MongoCollectionInitializer {

    private final MongoDatabase mongoDatabase;

    // Constructor injection
    public MongoCollectionInitializer(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    public void createCollectionIfNotExists(String collectionName, List<Map<String, Object>> fields) {
        if (!mongoDatabase.listCollectionNames().into(new ArrayList<>()).contains(collectionName)) {
            Document validationRules = generateValidationRules(fields);

            // Use CreateCollectionOptions to set validation rules
            CreateCollectionOptions options = new CreateCollectionOptions()
                    .validationOptions(new ValidationOptions().validator(validationRules));

            mongoDatabase.createCollection(collectionName, options);
            System.out.println("Created collection with validation: " + collectionName);
        }
    }

    private Document generateValidationRules(List<Map<String, Object>> fields) {
        Document properties = new Document();
        for (Map<String, Object> field : fields) {
            String fieldName = (String) field.get("fieldName");
            String fieldType = mapMongoType((String) field.get("type"));

            Document fieldValidation = new Document();
            fieldValidation.append("bsonType", fieldType);

            if (field.get("required") != null && (Boolean) field.get("required")) {
                fieldValidation.append("required", true);
            }

            properties.append(fieldName, fieldValidation);
        }

        return new Document("$jsonSchema", new Document("bsonType", "object").append("properties", properties));
    }

    private String mapMongoType(String yamlType) {
        return switch (yamlType) {
            case "String" -> "string";
            case "Number" -> "int";
            case "Date" -> "date";
            default -> throw new IllegalArgumentException("Unsupported type: " + yamlType);
        };
    }
}
