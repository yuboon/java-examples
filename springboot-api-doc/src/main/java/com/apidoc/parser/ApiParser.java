package com.apidoc.parser;

import com.apidoc.annotation.*;
import com.apidoc.example.ExampleGenerator;
import com.apidoc.model.*;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * API解析器 - 核心解析引擎
 */
@Component
public class ApiParser {

    private final ApplicationContext applicationContext;
    private final ExampleGenerator exampleGenerator;

    public ApiParser(ApplicationContext applicationContext, ExampleGenerator exampleGenerator) {
        this.applicationContext = applicationContext;
        this.exampleGenerator = exampleGenerator;
    }

    /**
     * 解析所有API
     */
    public ApiDocumentation parseAll(String environment) {
        List<Class<?>> controllers = scanControllers();

        List<ApiInfo> allApis = controllers.stream()
            .map(this::parseController)
            .flatMap(List::stream)
            .filter(api -> isVisibleInEnvironment(api, environment))
            .collect(Collectors.toList());

        // 按分组整理
        Map<String, List<ApiInfo>> groupedApis = allApis.stream()
            .collect(Collectors.groupingBy(api ->
                api.getGroup() != null ? api.getGroup() : "默认分组"));

        List<com.apidoc.model.ApiGroup> groups = groupedApis.entrySet().stream()
            .map(entry -> {
                com.apidoc.model.ApiGroup group = new com.apidoc.model.ApiGroup();
                group.setName(entry.getKey());
                group.setApis(entry.getValue());
                return group;
            })
            .sorted((g1, g2) -> {
                List<ApiInfo> apis1 = g1.getApis();
                List<ApiInfo> apis2 = g2.getApis();
                String name1 = apis1.isEmpty() ? "zzz" : apis1.get(0).getGroup();
                String name2 = apis2.isEmpty() ? "zzz" : apis2.get(0).getGroup();
                return name1.compareTo(name2);
            })
            .collect(Collectors.toList());

        ApiDocumentation doc = new ApiDocumentation();
        doc.setTitle("API 文档");
        doc.setDescription("自动生成的API文档");
        doc.setVersion("1.0.0");
        doc.setGroups(groups);
        doc.setAllApis(allApis);
        doc.setEnvironments(Arrays.asList("development", "test", "production", "all"));

        return doc;
    }

    /**
     * 扫描所有Controller
     */
    private List<Class<?>> scanControllers() {
        return applicationContext.getBeansWithAnnotation(RestController.class)
            .values()
            .stream()
            .map(Object::getClass)
            .collect(Collectors.toList());
    }

    /**
     * 解析单个Controller
     */
    private List<ApiInfo> parseController(Class<?> controllerClass) {
        // 解析类级别注解
        RequestMapping classMapping = controllerClass.getAnnotation(RequestMapping.class);
        com.apidoc.annotation.ApiGroup apiGroup = controllerClass.getAnnotation(com.apidoc.annotation.ApiGroup.class);

        final String basePath;
        if (classMapping != null && classMapping.value().length > 0) {
            basePath = classMapping.value()[0];
        } else {
            basePath = "";
        }

        final String groupName;
        if (apiGroup != null) {
            groupName = apiGroup.name();
        } else {
            groupName = "默认分组";
        }

        // 解析所有方法
        return Arrays.stream(controllerClass.getDeclaredMethods())
            .filter(this::isApiMethod)
            .map(method -> parseApiMethod(method, basePath, groupName))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * 判断是否为API方法
     */
    private boolean isApiMethod(Method method) {
        return method.isAnnotationPresent(GetMapping.class) ||
               method.isAnnotationPresent(PostMapping.class) ||
               method.isAnnotationPresent(PutMapping.class) ||
               method.isAnnotationPresent(DeleteMapping.class) ||
               method.isAnnotationPresent(PatchMapping.class) ||
               method.isAnnotationPresent(RequestMapping.class);
    }

    /**
     * 解析API方法
     */
    private ApiInfo parseApiMethod(Method method, String basePath, String groupName) {
        try {
            // 解析HTTP方法和路径
            HttpMethodInfo httpInfo = extractHttpInfo(method);
            if (httpInfo == null) return null;

            // 解析注解信息
            ApiOperation operation = method.getAnnotation(ApiOperation.class);
            ApiStatus status = method.getAnnotation(ApiStatus.class);
            ApiEnvironment environment = method.getAnnotation(ApiEnvironment.class);

            // 检查是否隐藏
            if (operation != null && operation.hidden()) {
                return null;
            }

            ApiInfo apiInfo = new ApiInfo();
            apiInfo.setMethod(httpInfo.getMethod());
            apiInfo.setPath(basePath + httpInfo.getPath());
            apiInfo.setSummary(operation != null ? operation.value() : generateMethodSummary(method));
            apiInfo.setDescription(operation != null ? operation.description() : "");
            apiInfo.setGroup(groupName);

            // 设置状态信息
            if (status != null) {
                apiInfo.setStatus(status.value().name());
                apiInfo.setStatusLabel(status.value().getLabel());
                apiInfo.setStatusCss(status.value().getCssClass());
            } else {
                apiInfo.setStatus("STABLE");
                apiInfo.setStatusLabel("稳定");
                apiInfo.setStatusCss("success");
            }

            // 设置环境信息
            if (environment != null) {
                apiInfo.setEnvironments(Arrays.asList(environment.value()));
            } else {
                apiInfo.setEnvironments(Arrays.asList("all"));
            }

            // 解析参数
            apiInfo.setParameters(parseParameters(method));

            // 解析返回类型
            apiInfo.setReturnType(parseReturnType(method));

            // 生成示例数据
            ApiExample example = method.getAnnotation(ApiExample.class);
            if (example != null) {
                try {
                    Map<String, Object> examples = exampleGenerator.generateExamples(example);
                    apiInfo.setExamples(examples);
                } catch (Exception e) {
                    System.err.println("生成示例数据失败: " + method.getName() + ", 错误: " + e.getMessage());
                }
            }

            return apiInfo;
        } catch (Exception e) {
            System.err.println("解析API方法失败: " + method.getName() + ", 错误: " + e.getMessage());
            return null;
        }
    }

    /**
     * 提取HTTP方法信息
     */
    private HttpMethodInfo extractHttpInfo(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) {
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            return new HttpMethodInfo("GET", getPath(mapping.value()));
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            PostMapping mapping = method.getAnnotation(PostMapping.class);
            return new HttpMethodInfo("POST", getPath(mapping.value()));
        } else if (method.isAnnotationPresent(PutMapping.class)) {
            PutMapping mapping = method.getAnnotation(PutMapping.class);
            return new HttpMethodInfo("PUT", getPath(mapping.value()));
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            DeleteMapping mapping = method.getAnnotation(DeleteMapping.class);
            return new HttpMethodInfo("DELETE", getPath(mapping.value()));
        } else if (method.isAnnotationPresent(PatchMapping.class)) {
            PatchMapping mapping = method.getAnnotation(PatchMapping.class);
            return new HttpMethodInfo("PATCH", getPath(mapping.value()));
        } else if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            String httpMethod = mapping.method().length > 0 ?
                mapping.method()[0].name() : "GET";
            return new HttpMethodInfo(httpMethod, getPath(mapping.value()));
        }
        return null;
    }

    /**
     * 获取路径
     */
    private String getPath(String[] paths) {
        if (paths.length > 0 && !paths[0].isEmpty()) {
            return paths[0].startsWith("/") ? paths[0] : "/" + paths[0];
        }
        return "";
    }

    /**
     * 解析参数
     */
    private List<ParameterInfo> parseParameters(Method method) {
        return Arrays.stream(method.getParameters())
            .map(this::parseParameter)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * 解析单个参数
     */
    private ParameterInfo parseParameter(Parameter parameter) {
        String name = getParameterName(parameter);
        String type = getParameterType(parameter);
        boolean required = isParameterRequired(parameter);
        String paramType = getParameterLocation(parameter);

        ParameterInfo paramInfo = new ParameterInfo(name, type, required);
        paramInfo.setParamType(paramType);

        // 检查自定义注解
        ApiParam apiParam = parameter.getAnnotation(ApiParam.class);
        if (apiParam != null) {
            if (!apiParam.name().isEmpty()) {
                paramInfo.setName(apiParam.name());
            }
            paramInfo.setDescription(apiParam.description());
            paramInfo.setExample(apiParam.example());
            paramInfo.setDefaultValue(apiParam.defaultValue());
            paramInfo.setRequired(apiParam.required());
        }

        // 解析复杂类型参数的字段信息（包括泛型）
        List<FieldInfo> fields = parseParameterFields(parameter, new HashSet<>());
        if (!fields.isEmpty()) {
            paramInfo.setFields(fields);
        }

        return paramInfo;
    }

    /**
     * 解析参数的字段信息（处理泛型）
     */
    private List<FieldInfo> parseParameterFields(Parameter parameter, Set<Class<?>> visited) {
        Class<?> paramClass = parameter.getType();
        java.lang.reflect.Type genericType = parameter.getParameterizedType();

        // 处理泛型集合类型
        if (List.class.isAssignableFrom(paramClass) || Set.class.isAssignableFrom(paramClass)) {
            return parseCollectionTypeFields(genericType, visited, paramClass.getSimpleName());
        }
        // 处理Map类型
        else if (Map.class.isAssignableFrom(paramClass)) {
            return parseMapTypeFields(genericType, visited);
        }
        // 处理数组类型
        else if (paramClass.isArray()) {
            Class<?> componentType = paramClass.getComponentType();
            return parseClassFields(componentType, visited);
        }
        // 处理普通对象类型
        else if (!isPrimitiveOrWrapper(paramClass) && !paramClass.getName().startsWith("java.")) {
            return parseClassFields(paramClass, visited);
        }

        return new ArrayList<>();
    }

    /**
     * 获取参数名
     */
    private String getParameterName(Parameter parameter) {
        // 检查Spring注解
        if (parameter.isAnnotationPresent(RequestParam.class)) {
            RequestParam param = parameter.getAnnotation(RequestParam.class);
            return !param.value().isEmpty() ? param.value() :
                   !param.name().isEmpty() ? param.name() : parameter.getName();
        } else if (parameter.isAnnotationPresent(PathVariable.class)) {
            PathVariable param = parameter.getAnnotation(PathVariable.class);
            return !param.value().isEmpty() ? param.value() :
                   !param.name().isEmpty() ? param.name() : parameter.getName();
        }
        return parameter.getName();
    }

    /**
     * 获取参数类型字符串
     */
    private String getParameterType(Parameter parameter) {
        Class<?> type = parameter.getType();
        java.lang.reflect.Type genericType = parameter.getParameterizedType();

        // 处理泛型类型显示
        if (genericType instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) genericType;
            java.lang.reflect.Type[] actualTypes = paramType.getActualTypeArguments();

            if (List.class.isAssignableFrom(type) && actualTypes.length > 0) {
                return "List<" + ((Class<?>) actualTypes[0]).getSimpleName() + ">";
            } else if (Set.class.isAssignableFrom(type) && actualTypes.length > 0) {
                return "Set<" + ((Class<?>) actualTypes[0]).getSimpleName() + ">";
            } else if (Map.class.isAssignableFrom(type) && actualTypes.length >= 2) {
                return "Map<" + ((Class<?>) actualTypes[0]).getSimpleName() + ", " + ((Class<?>) actualTypes[1]).getSimpleName() + ">";
            }
        }

        return type.getSimpleName();
    }

    /**
     * 获取参数位置类型
     */
    private String getParameterLocation(Parameter parameter) {
        if (parameter.isAnnotationPresent(RequestParam.class)) {
            // 检查是否为文件上传参数
            if (isFileParameter(parameter)) {
                return "file";
            }
            return "query";
        } else if (parameter.isAnnotationPresent(PathVariable.class)) {
            return "path";
        } else if (parameter.isAnnotationPresent(RequestBody.class)) {
            return "body";
        } else if (parameter.isAnnotationPresent(RequestHeader.class)) {
            return "header";
        } else if (parameter.isAnnotationPresent(RequestPart.class)) {
            return "file";
        }

        // 默认检查是否为文件类型
        if (isFileParameter(parameter)) {
            return "file";
        }

        return "query";
    }

    /**
     * 判断参数是否为文件参数
     */
    private boolean isFileParameter(Parameter parameter) {
        Class<?> type = parameter.getType();
        String typeName = type.getName();

        // 检查常见的文件类型
        return "org.springframework.web.multipart.MultipartFile".equals(typeName) ||
               "org.springframework.web.multipart.MultipartFile[]".equals(typeName) ||
               type.isArray() && "org.springframework.web.multipart.MultipartFile".equals(type.getComponentType().getName()) ||
               (List.class.isAssignableFrom(type) && isMultipartFileList(parameter));
    }

    /**
     * 检查是否为MultipartFile的List类型
     */
    private boolean isMultipartFileList(Parameter parameter) {
        java.lang.reflect.Type genericType = parameter.getParameterizedType();
        if (genericType instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) genericType;
            java.lang.reflect.Type[] actualTypes = paramType.getActualTypeArguments();

            if (actualTypes.length > 0 && actualTypes[0] instanceof Class<?>) {
                Class<?> elementType = (Class<?>) actualTypes[0];
                return "org.springframework.web.multipart.MultipartFile".equals(elementType.getName());
            }
        }
        return false;
    }

    /**
     * 判断参数是否必填
     */
    private boolean isParameterRequired(Parameter parameter) {
        if (parameter.isAnnotationPresent(RequestParam.class)) {
            return parameter.getAnnotation(RequestParam.class).required();
        } else if (parameter.isAnnotationPresent(PathVariable.class)) {
            return parameter.getAnnotation(PathVariable.class).required();
        }
        return !parameter.getType().equals(Optional.class);
    }

    /**
     * 解析返回类型
     */
    private ReturnTypeInfo parseReturnType(Method method) {
        Class<?> returnType = method.getReturnType();
        java.lang.reflect.Type genericReturnType = method.getGenericReturnType();

        ReturnTypeInfo info = new ReturnTypeInfo(getReturnTypeString(returnType, genericReturnType));

        // 解析字段信息
        List<FieldInfo> fields = parseReturnTypeFields(returnType, genericReturnType, new HashSet<>());
        info.setFields(fields);

        return info;
    }

    /**
     * 解析返回类型的字段信息（处理泛型）
     */
    private List<FieldInfo> parseReturnTypeFields(Class<?> returnType, java.lang.reflect.Type genericReturnType, Set<Class<?>> visited) {
        // 处理泛型集合类型
        if (List.class.isAssignableFrom(returnType) || Set.class.isAssignableFrom(returnType)) {
            return parseCollectionTypeFields(genericReturnType, visited, returnType.getSimpleName());
        }
        // 处理Map类型
        else if (Map.class.isAssignableFrom(returnType)) {
            return parseMapTypeFields(genericReturnType, visited);
        }
        // 处理数组类型
        else if (returnType.isArray()) {
            Class<?> componentType = returnType.getComponentType();
            return parseClassFields(componentType, visited);
        }
        // 处理普通对象类型
        else {
            return parseClassFields(returnType, visited);
        }
    }

    /**
     * 解析集合类型的字段信息
     */
    private List<FieldInfo> parseCollectionTypeFields(java.lang.reflect.Type genericType, Set<Class<?>> visited, String containerType) {
        if (genericType instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) genericType;
            java.lang.reflect.Type[] actualTypes = paramType.getActualTypeArguments();

            if (actualTypes.length > 0) {
                java.lang.reflect.Type elementType = actualTypes[0];
                if (elementType instanceof Class<?>) {
                    Class<?> elementClass = (Class<?>) elementType;
                    // 创建一个容器字段来表示集合元素
                    FieldInfo containerField = new FieldInfo();
                    containerField.setName(containerType + "元素");
                    containerField.setType(elementClass.getSimpleName());
                    containerField.setDescription("集合中的" + elementClass.getSimpleName() + "对象详情");

                    // 递归解析元素类型的字段
                    if (!isPrimitiveOrWrapper(elementClass) && !elementClass.getName().startsWith("java.")) {
                        List<FieldInfo> children = parseClassFields(elementClass, visited);
                        containerField.setChildren(children);
                    }

                    return List.of(containerField);
                }
            }
        }
        return new ArrayList<>();
    }

    /**
     * 解析Map类型的字段信息
     */
    private List<FieldInfo> parseMapTypeFields(java.lang.reflect.Type genericType, Set<Class<?>> visited) {
        if (genericType instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) genericType;
            java.lang.reflect.Type[] actualTypes = paramType.getActualTypeArguments();

            if (actualTypes.length >= 2) {
                java.lang.reflect.Type valueType = actualTypes[1];
                if (valueType instanceof Class<?>) {
                    Class<?> valueClass = (Class<?>) valueType;
                    // 创建一个容器字段来表示Map值
                    FieldInfo containerField = new FieldInfo();
                    containerField.setName("Map值");
                    containerField.setType(valueClass.getSimpleName());
                    containerField.setDescription("Map中的" + valueClass.getSimpleName() + "对象详情");

                    // 递归解析值类型的字段
                    if (!isPrimitiveOrWrapper(valueClass) && !valueClass.getName().startsWith("java.")) {
                        List<FieldInfo> children = parseClassFields(valueClass, visited);
                        containerField.setChildren(children);
                    }

                    return List.of(containerField);
                }
            }
        }
        return new ArrayList<>();
    }

    /**
     * 获取返回类型字符串（处理泛型显示）
     */
    private String getReturnTypeString(Class<?> returnType, java.lang.reflect.Type genericReturnType) {
        if (genericReturnType instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) genericReturnType;
            java.lang.reflect.Type[] actualTypes = paramType.getActualTypeArguments();

            if (List.class.isAssignableFrom(returnType) && actualTypes.length > 0) {
                return "List<" + ((Class<?>) actualTypes[0]).getSimpleName() + ">";
            } else if (Set.class.isAssignableFrom(returnType) && actualTypes.length > 0) {
                return "Set<" + ((Class<?>) actualTypes[0]).getSimpleName() + ">";
            } else if (Map.class.isAssignableFrom(returnType) && actualTypes.length >= 2) {
                return "Map<" + ((Class<?>) actualTypes[0]).getSimpleName() + ", " + ((Class<?>) actualTypes[1]).getSimpleName() + ">";
            }
        }

        return returnType.getSimpleName();
    }

    /**
     * 解析类的字段信息（支持嵌套对象）
     */
    private List<FieldInfo> parseClassFields(Class<?> clazz, Set<Class<?>> visited) {
        List<FieldInfo> fields = new ArrayList<>();

        // 防止循环引用
        if (visited.contains(clazz) || isPrimitiveOrWrapper(clazz) || clazz.getName().startsWith("java.")) {
            return fields;
        }

        visited.add(clazz);

        // 解析所有字段
        Arrays.stream(clazz.getDeclaredFields())
            .filter(field -> !field.isSynthetic() && !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
            .forEach(field -> {
                // 检查是否隐藏
                ApiField apiField = field.getAnnotation(ApiField.class);
                if (apiField != null && apiField.hidden()) {
                    return;
                }

                FieldInfo fieldInfo = new FieldInfo();

                // 设置字段名
                if (apiField != null && !apiField.name().isEmpty()) {
                    fieldInfo.setName(apiField.name());
                } else {
                    fieldInfo.setName(field.getName());
                }

                // 设置字段类型
                String fieldType = getFieldTypeString(field.getType(), field);
                fieldInfo.setType(fieldType);

                // 设置描述（优先使用注解）
                if (apiField != null && !apiField.value().isEmpty()) {
                    fieldInfo.setDescription(apiField.value());
                } else {
                    fieldInfo.setDescription(""); // 不提供默认描述
                }

                // 设置必填状态
                if (apiField != null) {
                    fieldInfo.setRequired(apiField.required());
                    fieldInfo.setExample(apiField.example());
                }

                // 处理复杂类型
                handleComplexType(field, fieldInfo, new HashSet<>(visited));

                fields.add(fieldInfo);
            });

        return fields;
    }

    /**
     * 处理复杂类型（List、Map、自定义对象等）
     */
    private void handleComplexType(java.lang.reflect.Field field, FieldInfo fieldInfo, Set<Class<?>> visited) {
        Class<?> fieldType = field.getType();

        // 处理 List 类型
        if (List.class.isAssignableFrom(fieldType)) {
            handleGenericType(field, fieldInfo, visited, "List");
        }
        // 处理 Map 类型
        else if (Map.class.isAssignableFrom(fieldType)) {
            handleGenericType(field, fieldInfo, visited, "Map");
        }
        // 处理数组类型
        else if (fieldType.isArray()) {
            Class<?> componentType = fieldType.getComponentType();
            fieldInfo.setType(getFieldTypeString(componentType, null) + "[]");

            if (!isPrimitiveOrWrapper(componentType) && !componentType.getName().startsWith("java.")) {
                List<FieldInfo> children = parseClassFields(componentType, visited);
                if (!children.isEmpty()) {
                    fieldInfo.setChildren(children);
                }
            }
        }
        // 处理自定义对象
        else if (!isPrimitiveOrWrapper(fieldType) && !fieldType.getName().startsWith("java.")) {
            List<FieldInfo> children = parseClassFields(fieldType, visited);
            if (!children.isEmpty()) {
                fieldInfo.setChildren(children);
            }
        }
    }

    /**
     * 处理泛型类型
     */
    private void handleGenericType(java.lang.reflect.Field field, FieldInfo fieldInfo, Set<Class<?>> visited, String containerType) {
        java.lang.reflect.Type genericType = field.getGenericType();

        if (genericType instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) genericType;
            java.lang.reflect.Type[] actualTypes = paramType.getActualTypeArguments();

            if (actualTypes.length > 0) {
                java.lang.reflect.Type actualType = actualTypes[0];

                if (actualType instanceof Class<?>) {
                    Class<?> actualClass = (Class<?>) actualType;

                    if ("Map".equals(containerType) && actualTypes.length > 1) {
                        // Map<K,V> 类型
                        java.lang.reflect.Type valueType = actualTypes[1];
                        if (valueType instanceof Class<?>) {
                            Class<?> valueClass = (Class<?>) valueType;
                            fieldInfo.setType("Map<" + actualClass.getSimpleName() + ", " + valueClass.getSimpleName() + ">");

                            // 如果值类型是自定义对象，递归解析
                            if (!isPrimitiveOrWrapper(valueClass) && !valueClass.getName().startsWith("java.")) {
                                List<FieldInfo> children = parseClassFields(valueClass, visited);
                                if (!children.isEmpty()) {
                                    fieldInfo.setChildren(children);
                                }
                            }
                        }
                    } else {
                        // List<T> 类型
                        fieldInfo.setType("List<" + actualClass.getSimpleName() + ">");

                        // 如果元素类型是自定义对象，递归解析
                        if (!isPrimitiveOrWrapper(actualClass) && !actualClass.getName().startsWith("java.")) {
                            List<FieldInfo> children = parseClassFields(actualClass, visited);
                            if (!children.isEmpty()) {
                                fieldInfo.setChildren(children);
                            }
                        }
                    }
                }
            }
        } else {
            // 无泛型的情况
            fieldInfo.setType(containerType);
        }
    }

    /**
     * 判断是否为基础类型或包装类型
     */
    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ||
               clazz == String.class ||
               clazz == Integer.class || clazz == Long.class ||
               clazz == Double.class || clazz == Float.class ||
               clazz == Boolean.class || clazz == Character.class ||
               clazz == Byte.class || clazz == Short.class ||
               clazz.isEnum();
    }

    /**
     * 获取字段类型字符串
     */
    private String getFieldTypeString(Class<?> type, java.lang.reflect.Field field) {
        if (type == Integer.class || type == int.class) return "integer";
        if (type == Long.class || type == long.class) return "long";
        if (type == Double.class || type == double.class) return "double";
        if (type == Float.class || type == float.class) return "float";
        if (type == Boolean.class || type == boolean.class) return "boolean";
        if (type == String.class) return "string";
        if (type.isEnum()) return "enum";
        if (List.class.isAssignableFrom(type)) return "List";
        if (Map.class.isAssignableFrom(type)) return "Map";
        if (type.isArray()) return type.getComponentType().getSimpleName() + "[]";
        return type.getSimpleName();
    }

    // 重载方法保持兼容性
    private String getFieldTypeString(Class<?> type) {
        return getFieldTypeString(type, null);
    }

    /**
     * 生成方法摘要
     */
    private String generateMethodSummary(Method method) {
        String methodName = method.getName();

        // 基于方法名生成描述
        if (methodName.startsWith("get") || methodName.startsWith("find") || methodName.startsWith("list")) {
            return "获取数据";
        } else if (methodName.startsWith("create") || methodName.startsWith("add") || methodName.startsWith("save")) {
            return "创建数据";
        } else if (methodName.startsWith("update") || methodName.startsWith("modify") || methodName.startsWith("edit")) {
            return "更新数据";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "删除数据";
        }

        return methodName;
    }

    /**
     * 检查API在指定环境下是否可见
     */
    private boolean isVisibleInEnvironment(ApiInfo api, String environment) {
        if (environment == null || environment.equals("all")) {
            return true;
        }

        List<String> environments = api.getEnvironments();
        if (environments == null || environments.isEmpty()) {
            return true;
        }

        return environments.contains("all") || environments.contains(environment);
    }

    /**
     * HTTP方法信息内部类
     */
    private static class HttpMethodInfo {
        private final String method;
        private final String path;

        public HttpMethodInfo(String method, String path) {
            this.method = method;
            this.path = path;
        }

        public String getMethod() { return method; }
        public String getPath() { return path; }
    }
}