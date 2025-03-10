package com.scm_saas.SCM_SaaS.service;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DynamicApiRegistration {

    public void registerApis(List<Map<String, Object>> entities) {
        for (Map<String, Object> entity : entities) {
            String entityName = (String) entity.get("name");
            String endpoint = "/api/dynamic/" + entityName.toLowerCase();

            // Ensure handlerMapping is used in endpoint registration logic
            System.out.println("Registering endpoint: " + endpoint);
        }
    }
}
