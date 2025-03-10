package com.scm_saas.SCM_SaaS.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoConfig {

    private static final String MONGO_URI = "mongodb://localhost:27017"; // Replace with your connection string
    private static final String DATABASE_NAME = "scm_saas"; // Replace with your database name
    private static final Logger logger = LoggerFactory.getLogger(MongoConfig.class);


    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(MONGO_URI);
    }

    @Bean
    public MongoDatabase mongoDatabase(MongoClient mongoClient) {
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        // Log MongoDB database details
        logger.info("Connected to MongoDB database: {}", DATABASE_NAME);
        return database;
    }
}