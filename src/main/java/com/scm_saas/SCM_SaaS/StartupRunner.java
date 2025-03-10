package com.scm_saas.SCM_SaaS;

import com.scm_saas.SCM_SaaS.service.DynamicApiRegistration;
import com.scm_saas.SCM_SaaS.service.DynamicClassGeneration;
import com.scm_saas.SCM_SaaS.service.MongoCollectionInitializer;
import com.scm_saas.SCM_SaaS.util.YamlParserUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class StartupRunner implements CommandLineRunner {

    private final YamlParserUtil yamlParserUtil;
    private final DynamicClassGeneration dynamicClassGenerator;
    private final DynamicApiRegistration dynamicApiRegistrar;
    private final MongoCollectionInitializer mongoCollectionInitializer;

    // Constructor injection
    public StartupRunner(YamlParserUtil yamlParserUtil,
                         DynamicClassGeneration dynamicClassGenerator,
                         DynamicApiRegistration dynamicApiRegistrar,
                         MongoCollectionInitializer mongoCollectionInitializer) {
        this.yamlParserUtil = yamlParserUtil;
        this.dynamicClassGenerator = dynamicClassGenerator;
        this.dynamicApiRegistrar = dynamicApiRegistrar;
        this.mongoCollectionInitializer = mongoCollectionInitializer;
    }

    @Override
    public void run(String... args) throws Exception {
        String yamlFilePath = "src/main/resources/templates/base.yaml";

        // Parse and validate YAML
        List<Map<String, Object>> entities = yamlParserUtil.parseAndValidateYaml(yamlFilePath);

        // Generate Java classes dynamically
        dynamicClassGenerator.generateClasses(entities);

        // Register REST APIs for entities
        dynamicApiRegistrar.registerApis(entities);

        entities.forEach(entity -> {
            String collectionName = entity.get("name").toString();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fields = (List<Map<String, Object>>) entity.get("fields");
            mongoCollectionInitializer.createCollectionIfNotExists(collectionName, fields);
        });

        System.out.println("System initialized successfully from base.yaml!");
    }
}
