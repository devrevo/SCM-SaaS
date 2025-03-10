package com.scm_saas.SCM_SaaS.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class YamlParserUtil {


    public List<Map<String, Object>> parseAndValidateYaml(String filePath) throws IOException {
        List<Map<String, Object>> entities = parseYaml(filePath);
        validateYamlStructure(entities);
        return entities;
    }

    private List<Map<String, Object>> parseYaml(String yamlFilePath) throws IOException {
        // Parse YAML file into a generic structure
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map<String, Object> yamlData = mapper.readValue(new File(yamlFilePath), Map.class);

        // Extract entities or equivalent sections
        return (List<Map<String, Object>>) yamlData.get("entities");
    }
    private void validateYamlStructure(List<Map<String, Object>> entities) {
        for (Map<String, Object> entity : entities) {
            if (!entity.containsKey("name") || !entity.containsKey("fields")) {
                throw new IllegalArgumentException("Invalid YAML structure: 'name' and 'fields' are required.");
            }
        }
    }

}
