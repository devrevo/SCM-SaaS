package com.scm_saas.SCM_SaaS.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.scm_saas.SCM_SaaS.util.DynamicEntitySerializer;
import com.scm_saas.SCM_SaaS.util.MongoHelper;
import jakarta.annotation.PostConstruct;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

@RestController
@RequestMapping("/api/dynamic")
public class DynamicApiController {

    private final MongoDatabase mongoDatabase;
    private final MongoHelper mongoHelper;
    private final ObjectMapper objectMapper;

    // Inject the ObjectMapper and register the custom serializer
    @PostConstruct
    public void configureObjectMapper() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Object.class, new DynamicEntitySerializer());

        // Register Java 8 time module if using modern date/time types
        objectMapper.registerModule(new JavaTimeModule());
        // Register the custom serializer module
        objectMapper.registerModule(module);
        // Configure other settings
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    // Constructor injection
    public DynamicApiController(MongoDatabase mongoDatabase, MongoHelper mongoHelper, ObjectMapper objectMapper) {
        this.mongoDatabase = mongoDatabase;
        this.mongoHelper = mongoHelper;
        this.objectMapper = objectMapper;
    }

    // Create a new document
    @PostMapping("/{collectionName}")
    public ResponseEntity<String> createDocument(
            @PathVariable String collectionName,
            @RequestBody Document document) {
        try {
            mongoDatabase.getCollection(collectionName).insertOne(document);
            System.out.println("Document added to collection: " + collectionName);
            return ResponseEntity.ok("Document added to " + collectionName);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to add document to " + collectionName);
        }
    }

    // Create multiple documents
    @PostMapping("/{collectionName}/batch")
    public ResponseEntity<String> createMultipleDocuments(
            @PathVariable String collectionName,
            @RequestBody List<Document> documents) {
        try {
            mongoDatabase.getCollection(collectionName).insertMany(documents);
            System.out.println(documents.size() + " documents added to collection: " + collectionName);
            return ResponseEntity.ok(documents.size() + " documents added to " + collectionName);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to add documents to " + collectionName);
        }
    }

    // Retrieve all documents
   /* @GetMapping("/{collectionName}")
    public ResponseEntity<List<Object>> getAllDocuments(@PathVariable String collectionName) {
        List<Object> results = new ArrayList<>();
        try (MongoCursor<Document> cursor = mongoDatabase.getCollection(collectionName).find().iterator()) {
            String className = "com.scm_saas.entity." + capitalize(collectionName);
            Class<?> entityClass = Class.forName(className);

            cursor.forEachRemaining(doc -> {
                Object entity = mongoHelper.mapDocumentToEntity(doc, entityClass);
                results.add(objectMapper.convertValue(entity, Map.class));
            });
            System.out.println("Fetched " + results.size() + " documents from collection: " + collectionName);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.ok(results);
    }
*/
    // Retrieve multiple documents by batch
//    @GetMapping("/{collectionName}/batch")
//    public ResponseEntity<List<Object>> getDocumentsByBatch(
//            @PathVariable String collectionName,
//            @RequestParam List<String> ids) {
//        List<Object> results = new ArrayList<>();
//        try {
//            String className = "com.scm_saas.entity." + capitalize(collectionName);
//            Class<?> entityClass = Class.forName(className);
//
//            Bson query = in("_id", ids);
//            MongoCursor<Document> cursor = mongoDatabase.getCollection(collectionName).find(query).iterator();
//            cursor.forEachRemaining(doc -> {
//                Object entity = mongoHelper.mapDocumentToEntity(doc, entityClass);
//                results.add(objectMapper.convertValue(entity, Map.class));
//            });
//            System.out.println("Fetched " + results.size() + " documents by batch from collection: " + collectionName);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(500).build();
//        }
//        return ResponseEntity.ok(results);
//    }

    // Retrieve a single document by ID
    @GetMapping("/{collectionName}/{id}")
    public ResponseEntity<Object> getDocumentById(
            @PathVariable String collectionName,
            @PathVariable String id) {
        try {
            Document document = mongoDatabase.getCollection(collectionName).find(eq("_id", id)).first();
            if (document != null) {
                String className = "com.scm_saas.entity." + capitalize(collectionName);
                Class<?> entityClass = Class.forName(className);
                Object entity = mongoHelper.mapDocumentToEntity(document, entityClass);
                System.out.println("READ - Document fetched from collection " + collectionName + ": " + document.toJson());
                return ResponseEntity.ok(objectMapper.convertValue(entity, Map.class));
            } else {
                System.out.println("READ - Document with ID " + id + " not found in collection: " + collectionName);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // Update a single document by ID
    @PutMapping("/{collectionName}/{id}")
    public ResponseEntity<String> updateDocument(
            @PathVariable String collectionName,
            @PathVariable String id,
            @RequestBody Document updates) {
        try {
            Bson updateOperations = combine(updates);
            var result = mongoDatabase.getCollection(collectionName).updateOne(eq("_id", id), updateOperations);
            if (result.getMatchedCount() > 0) {
                System.out.println("UPDATE - Document with ID " + id + " updated in collection " + collectionName);
                return ResponseEntity.ok("Document with ID " + id + " updated successfully.");
            } else {
                System.out.println("UPDATE - Document with ID " + id + " not found in collection: " + collectionName);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to update document with ID " + id);
        }
    }

    // Update multiple documents
    @PutMapping("/{collectionName}/batch")
    public ResponseEntity<String> updateMultipleDocuments(
            @PathVariable String collectionName,
            @RequestBody List<Document> updates) {
        try {
            for (Document update : updates) {
                String id = update.getString("_id");
                update.remove("_id");
                Bson updateOperations = combine(update);
                mongoDatabase.getCollection(collectionName).updateOne(eq("_id", id), updateOperations);
            }
            System.out.println("Updated " + updates.size() + " documents in collection: " + collectionName);
            return ResponseEntity.ok("Updated " + updates.size() + " documents in " + collectionName);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to update documents in " + collectionName);
        }
    }

    // Delete a single document by ID
    @DeleteMapping("/{collectionName}/{id}")
    public ResponseEntity<String> deleteDocument(
            @PathVariable String collectionName,
            @PathVariable String id) {
        try {
            var result = mongoDatabase.getCollection(collectionName).deleteOne(eq("_id", id));
            if (result.getDeletedCount() > 0) {
                System.out.println("DELETE - Document with ID " + id + " deleted from collection: " + collectionName);
                return ResponseEntity.ok("Document with ID " + id + " deleted successfully.");
            } else {
                System.out.println("DELETE - Document with ID " + id + " not found in collection: " + collectionName);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to delete document with ID " + id);
        }
    }

    // Delete multiple documents
    @DeleteMapping("/{collectionName}/batch")
    public ResponseEntity<String> deleteMultipleDocuments(
            @PathVariable String collectionName,
            @RequestBody List<String> ids) {
        try {
            for (String id : ids) {
                mongoDatabase.getCollection(collectionName).deleteOne(eq("_id", id));
            }
            System.out.println("Deleted " + ids.size() + " documents from collection: " + collectionName);
            return ResponseEntity.ok("Deleted " + ids.size() + " documents from " + collectionName);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to delete documents from " + collectionName);
        }
    }

    // Search documents based on field and value
    @GetMapping("/{collectionName}/search")
    public ResponseEntity<List<Object>> searchDocuments(
            @PathVariable String collectionName,
            @RequestParam String field,
            @RequestParam String value) {
        List<Object> results = new ArrayList<>();
        try {
            String className = "com.scm_saas.entity." + capitalize(collectionName);
            Class<?> entityClass = Class.forName(className);

            Field entityField = entityClass.getDeclaredField(field);
            Bson query;

            if (entityField.getType().equals(String.class)) {
                query = eq(field, value);
            } else if (entityField.getType().equals(Integer.class)) {
                query = eq(field, Integer.parseInt(value));
            } else if (entityField.getType().equals(Double.class)) {
                query = eq(field, Double.parseDouble(value));
            } else {
                query = eq(field, value); // Default to string equality
            }

            for (Document document : mongoDatabase.getCollection(collectionName).find(query)) {
                System.out.println("Fetched document: " + document.toJson()); // Log fetched document
                Object entity = mongoHelper.mapDocumentToEntity(document, entityClass);
                System.out.println("Mapped entity: " + entity);

                results.add(entity);
            }

            System.out.println("Search returned " + results.size() + " documents from collection: " + collectionName);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.ok(results);
    }

    // Helper to capitalize the collection name for dynamic class lookup
    private String capitalize(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
