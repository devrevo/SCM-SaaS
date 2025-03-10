package com.scm_saas.SCM_SaaS.service;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DynamicClassGeneration {

    private static final String BASE_PACKAGE = "com.scm_saas.entity";

    public void generateClasses(List<Map<String, Object>> entities) throws NotFoundException {
        ClassPool pool = ClassPool.getDefault();
        pool.importPackage("com.fasterxml.jackson.annotation");

        for (Map<String, Object> entity : entities) {
            String className = BASE_PACKAGE + "." + entity.get("name");

            CtClass ctClass;
            try {
                ctClass = pool.get(className);
                if (ctClass.isFrozen()) {
                    System.out.println("Class " + className + " already exists and is frozen. Skipping.");
                    continue;
                }
            } catch (NotFoundException e) {
                ctClass = pool.makeClass(className);
                System.out.println("Creating new class: " + className);

                // Add class-level Jackson annotations
                addClassLevelJacksonAnnotations(ctClass);
            }

            // Add fields dynamically
            Object fieldsObject = entity.get("fields");
            if (fieldsObject instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> fields = (List<Map<String, Object>>) fieldsObject;
                for (Map<String, Object> field : fields) {
                    String fieldName = (String) field.get("fieldName");
                    String fieldType = mapFieldType((String) field.get("type"));

                    if (fieldExists(ctClass, fieldName)) {
                        continue;
                    }

                    try {
                        CtField ctField = new CtField(pool.get(fieldType), fieldName, ctClass);
                        addJacksonAnnotations(pool, ctClass, ctField);
                        ctClass.addField(ctField);
                    } catch (Exception ex) {

                        System.err.println("Failed to add field " + fieldName + ": " + ex.getMessage());
                    }
                }
            }
            // Add additional methods and constructors
            addToStringMethod(ctClass);
            addNoArgsConstructor(ctClass);
            logGeneratedFields(ctClass);

            try {
                ctClass.writeFile("target/classes");
                System.out.println("Class " + className + " written to bytecode.");
            } catch (Exception ex) {
                System.err.println("Failed to write class " + className + " to bytecode: " + ex.getMessage());
            }
        }
    }

    private void addJacksonAnnotations(ClassPool pool, CtClass ctClass, CtField ctField) throws NotFoundException {
        ConstPool constPool = ctClass.getClassFile().getConstPool();
        AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);

        // Add JsonProperty annotation
        Annotation jsonProp = new Annotation("com.fasterxml.jackson.annotation.JsonProperty", constPool);
        jsonProp.addMemberValue("value", new StringMemberValue(ctField.getName(), constPool));

        // If it's a Date field, add JsonFormat annotation
        if (ctField.getType().getName().equals("java.util.Date")) {
            Annotation jsonFormat = new Annotation("com.fasterxml.jackson.annotation.JsonFormat", constPool);
            jsonFormat.addMemberValue("pattern", new StringMemberValue("yyyy-MM-dd", constPool));
            attr.addAnnotation(jsonFormat);
        }

        attr.addAnnotation(jsonProp);
        ctField.getFieldInfo().addAttribute(attr);
    }

    private void addClassLevelJacksonAnnotations(CtClass ctClass) {
        try {
            ConstPool constPool = ctClass.getClassFile().getConstPool();
            AnnotationsAttribute classAttr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);

            // Add @JsonIdentityInfo annotation to prevent infinite recursion
            Annotation jsonIdentityInfo = new Annotation("com.fasterxml.jackson.annotation.JsonIdentityInfo", constPool);
            // Set generator to PropertyGenerator
            EnumMemberValue generator = new EnumMemberValue(constPool);
            generator.setType("com.fasterxml.jackson.annotation.ObjectIdGenerators$PropertyGenerator");
            generator.setValue("PropertyGenerator");
            jsonIdentityInfo.addMemberValue("generator", generator);
            // Set property to "_id"
            StringMemberValue property = new StringMemberValue("_id", constPool);
            jsonIdentityInfo.addMemberValue("property", property);

            // Add @JsonInclude annotation
            Annotation jsonInclude = new Annotation("com.fasterxml.jackson.annotation.JsonInclude", constPool);
            EnumMemberValue includeValue = new EnumMemberValue(constPool);
            includeValue.setType("com.fasterxml.jackson.annotation.JsonInclude$Include");
            includeValue.setValue("NON_NULL");
            jsonInclude.addMemberValue("value", includeValue);

            classAttr.addAnnotation(jsonIdentityInfo);
            classAttr.addAnnotation(jsonInclude);
            ctClass.getClassFile().addAttribute(classAttr);

            // Add MongoDB @Document annotation
            AnnotationsAttribute documentAttr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
            Annotation documentAnnotation = new Annotation("org.springframework.data.mongodb.core.mapping.Document", constPool);
            documentAttr.addAnnotation(documentAnnotation);
            ctClass.getClassFile().addAttribute(documentAttr);

        } catch (Exception e) {
            System.err.println("Failed to add class-level Jackson annotations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addToStringMethod(CtClass ctClass) {
        try {
            CtMethod existingMethod = ctClass.getDeclaredMethod("toString");
            if (existingMethod != null) {
                System.out.println("toString method already exists in class " + ctClass.getName() + ". Skipping...");
                return;
            }

            StringBuilder methodBody = new StringBuilder();
            methodBody.append("{ return \"")
                    .append(ctClass.getSimpleName())
                    .append(" [");

            CtField[] fields = ctClass.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                String fieldName = fields[i].getName();
                if (i > 0) {
                    methodBody.append(" + \", ");
                }
                methodBody.append(fieldName).append("=\" + ")
                        .append("String.valueOf(this.").append(fieldName).append(")");
            }

            methodBody.append(" + \"]\"; }");

            CtMethod toStringMethod = CtNewMethod.make(
                    "public String toString()" + methodBody.toString(),
                    ctClass);
            ctClass.addMethod(toStringMethod);
            System.out.println("Added toString method to class " + ctClass.getName());
        } catch (Exception e) {
            System.err.println("Failed to add toString method to class " + ctClass.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addNoArgsConstructor(CtClass ctClass) {
        try {
            for (CtConstructor constructor : ctClass.getConstructors()) {
                if (constructor.getParameterTypes().length == 0) {
                    System.out.println("No-arg constructor already exists for class " + ctClass.getName());
                    return; // No-arg constructor already exists
                }
            }

            CtConstructor noArgConstructor = new CtConstructor(new CtClass[]{}, ctClass);
            noArgConstructor.setBody("{}");
            ctClass.addConstructor(noArgConstructor);
            System.out.println("Added no-arg constructor to class " + ctClass.getName());
        } catch (Exception e) {
            System.err.println("Failed to add no-arg constructor to class " + ctClass.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String mapFieldType(String yamlType) {
        return switch (yamlType) {
            case "String" -> "java.lang.String";
            case "Number" -> "java.lang.Double"; // Or Integer based on use case
            case "Date" -> "java.util.Date";
            default -> throw new IllegalArgumentException("Unsupported type: " + yamlType);
        };
    }

    private boolean fieldExists(CtClass ctClass, String fieldName) {
        try {
            ctClass.getDeclaredField(fieldName);
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    private void logGeneratedFields(CtClass ctClass) {
        System.out.println("Generated class: " + ctClass.getName());
        for (CtField field : ctClass.getDeclaredFields()) {
            try {
                System.out.println("Field: " + field.getName() + ", Type: " + field.getType().getName());
            } catch (Exception e) {
                System.err.println("Error retrieving field details for: " + field.getName() + ", error: " + e.getMessage());
            }
        }
    }
}
