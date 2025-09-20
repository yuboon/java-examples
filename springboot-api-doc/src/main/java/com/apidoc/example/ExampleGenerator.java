package com.apidoc.example;

import com.apidoc.annotation.ApiExample;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能示例数据生成器
 * 基于字段名和类型生成合理的示例数据
 */
@Component
public class ExampleGenerator {

    private final Map<Class<?>, Object> exampleCache = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /**
     * 生成示例数据
     */
    public Object generateExample(Class<?> type) {
        return generateExample(type, true, "default");
    }

    /**
     * 生成示例数据
     *
     * @param type 数据类型
     * @param realistic 是否生成真实数据
     * @param scenario 场景名称
     */
    public Object generateExample(Class<?> type, boolean realistic, String scenario) {
        if (type == null) {
            return null;
        }

        // 检查缓存
        String cacheKey = type.getName() + "_" + realistic + "_" + scenario;
        if (exampleCache.containsKey(type) && realistic) {
            return exampleCache.get(type);
        }

        Object example = createExample(type, realistic, scenario);

        if (realistic && example != null) {
            exampleCache.put(type, example);
        }

        return example;
    }

    /**
     * 根据ApiExample注解生成示例
     */
    public Map<String, Object> generateExamples(ApiExample apiExample) {
        Map<String, Object> examples = new HashMap<>();

        Class<?> exampleClass = apiExample.value();
        String scenario = apiExample.scenario();
        boolean realistic = apiExample.realistic();

        // 生成主要示例
        Object mainExample = generateExample(exampleClass, realistic, scenario);
        examples.put("success", mainExample);

        // 生成错误示例
        if (!"error".equals(scenario)) {
            examples.put("error", generateErrorExample());
        }

        return examples;
    }

    /**
     * 创建示例对象
     */
    private Object createExample(Class<?> type, boolean realistic, String scenario) {
        // 基础类型
        if (type.isPrimitive() || isWrapperType(type)) {
            return generatePrimitiveExample(type, realistic);
        }

        // 字符串类型
        if (type == String.class) {
            return realistic ? "示例文本" : "string";
        }

        // 日期类型
        if (type == Date.class) {
            return new Date();
        }
        if (type == LocalDate.class) {
            return LocalDate.now();
        }
        if (type == LocalDateTime.class) {
            return LocalDateTime.now();
        }

        // 集合类型
        if (List.class.isAssignableFrom(type)) {
            return Collections.singletonList(realistic ? "列表项目" : "item");
        }
        if (Set.class.isAssignableFrom(type)) {
            return Collections.singleton(realistic ? "集合项目" : "item");
        }
        if (Map.class.isAssignableFrom(type)) {
            Map<String, Object> map = new HashMap<>();
            map.put("key", realistic ? "示例值" : "value");
            return map;
        }

        // 数组类型
        if (type.isArray()) {
            Class<?> componentType = type.getComponentType();
            Object array = java.lang.reflect.Array.newInstance(componentType, 1);
            java.lang.reflect.Array.set(array, 0, generateExample(componentType, realistic, scenario));
            return array;
        }

        // 复杂对象
        return createObjectExample(type, realistic, scenario);
    }

    /**
     * 生成基础类型示例
     */
    private Object generatePrimitiveExample(Class<?> type, boolean realistic) {
        if (type == boolean.class || type == Boolean.class) {
            return true;
        }
        if (type == byte.class || type == Byte.class) {
            return (byte) (realistic ? random.nextInt(100) : 1);
        }
        if (type == short.class || type == Short.class) {
            return (short) (realistic ? random.nextInt(1000) : 100);
        }
        if (type == int.class || type == Integer.class) {
            return realistic ? random.nextInt(10000) + 1 : 123;
        }
        if (type == long.class || type == Long.class) {
            return realistic ? random.nextLong() % 100000L + 1000L : 123L;
        }
        if (type == float.class || type == Float.class) {
            return realistic ? round(random.nextFloat() * 1000, 2) : 123.45f;
        }
        if (type == double.class || type == Double.class) {
            return realistic ? round(random.nextDouble() * 1000, 2) : 123.45;
        }
        if (type == BigDecimal.class) {
            return realistic ? BigDecimal.valueOf(round(random.nextDouble() * 1000, 2)) : new BigDecimal("123.45");
        }
        if (type == char.class || type == Character.class) {
            return realistic ? (char) ('A' + random.nextInt(26)) : 'A';
        }

        return null;
    }

    /**
     * 创建复杂对象示例
     */
    private Object createObjectExample(Class<?> type, boolean realistic, String scenario) {
        try {
            Object instance = type.getDeclaredConstructor().newInstance();
            Field[] fields = type.getDeclaredFields();

            for (Field field : fields) {
                // 跳过静态字段
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                field.setAccessible(true);
                Object value = generateFieldValue(field, realistic, scenario);
                field.set(instance, value);
            }

            return instance;
        } catch (Exception e) {
            // 如果无法创建实例，返回简单的Map
            return createSimpleMap(type, realistic);
        }
    }

    /**
     * 生成字段值
     */
    private Object generateFieldValue(Field field, boolean realistic, String scenario) {
        String fieldName = field.getName().toLowerCase();
        Class<?> fieldType = field.getType();

        if (!realistic) {
            return generateExample(fieldType, false, scenario);
        }

        // 基于字段名智能推断
        Object smartValue = generateSmartValue(fieldName, fieldType);
        if (smartValue != null) {
            return smartValue;
        }

        // 回退到类型默认值
        return generateExample(fieldType, true, scenario);
    }

    /**
     * 基于字段名生成智能值
     */
    private Object generateSmartValue(String fieldName, Class<?> fieldType) {
        // ID相关
        if (fieldName.contains("id")) {
            if (fieldType == String.class) {
                return UUID.randomUUID().toString().substring(0, 8);
            }
            return 1000L + random.nextInt(9000);
        }

        // 名称相关
        if (fieldName.contains("name")) {
            if (fieldName.contains("user") || fieldName.contains("person")) {
                return pickRandom("张三", "李四", "王五", "赵六");
            }
            if (fieldName.contains("company") || fieldName.contains("org")) {
                return pickRandom("示例公司", "测试企业", "样例机构");
            }
            if (fieldName.contains("product") || fieldName.contains("item")) {
                return pickRandom("示例商品", "测试产品", "样例物品");
            }
            return "示例名称";
        }

        // 邮箱相关
        if (fieldName.contains("email") || fieldName.contains("mail")) {
            return pickRandom("user@example.com", "test@demo.com", "sample@test.org");
        }

        // 手机号相关
        if (fieldName.contains("phone") || fieldName.contains("mobile") || fieldName.contains("tel")) {
            return "138" + String.format("%08d", random.nextInt(100000000));
        }

        // 年龄相关
        if (fieldName.contains("age")) {
            return 18 + random.nextInt(50);
        }

        // 价格/金额相关
        if (fieldName.contains("price") || fieldName.contains("amount") || fieldName.contains("money")) {
            if (fieldType == BigDecimal.class) {
                return BigDecimal.valueOf(random.nextDouble() * 1000 + 1).setScale(2, BigDecimal.ROUND_HALF_UP);
            }
            return round(random.nextDouble() * 1000 + 1, 2);
        }

        // 数量相关
        if (fieldName.contains("count") || fieldName.contains("num") || fieldName.contains("quantity")) {
            return 1 + random.nextInt(100);
        }

        // 状态相关
        if (fieldName.contains("status") || fieldName.contains("state")) {
            if (fieldType == String.class) {
                return pickRandom("active", "inactive", "pending", "completed");
            }
            return random.nextInt(3);
        }

        // 地址相关
        if (fieldName.contains("address") || fieldName.contains("addr")) {
            return pickRandom("北京市朝阳区", "上海市浦东新区", "广州市天河区", "深圳市南山区");
        }

        // 描述相关
        if (fieldName.contains("desc") || fieldName.contains("comment") || fieldName.contains("remark")) {
            return "这是一段示例描述文本";
        }

        // 时间相关
        if (fieldName.contains("time") || fieldName.contains("date")) {
            if (fieldType == String.class) {
                return "2024-01-15 10:30:00";
            }
            if (fieldType == Date.class) {
                return new Date();
            }
            if (fieldType == LocalDateTime.class) {
                return LocalDateTime.now();
            }
            if (fieldType == LocalDate.class) {
                return LocalDate.now();
            }
        }

        // URL相关
        if (fieldName.contains("url") || fieldName.contains("link")) {
            return "https://example.com/sample";
        }

        return null;
    }

    /**
     * 创建简单Map (当无法创建对象实例时的回退方案)
     */
    private Map<String, Object> createSimpleMap(Class<?> type, boolean realistic) {
        Map<String, Object> map = new HashMap<>();
        String typeName = type.getSimpleName().toLowerCase();

        if (realistic) {
            map.put("id", 1001);
            map.put("name", "示例" + type.getSimpleName());
            map.put("description", "这是一个示例对象");
        } else {
            map.put("field1", "value1");
            map.put("field2", "value2");
        }

        return map;
    }

    /**
     * 生成错误示例
     */
    private Object generateErrorExample() {
        Map<String, Object> error = new HashMap<>();
        error.put("code", 400);
        error.put("message", "请求参数错误");
        error.put("data", null);
        error.put("timestamp", new Date());
        return error;
    }

    /**
     * 判断是否为包装类型
     */
    private boolean isWrapperType(Class<?> type) {
        return type == Boolean.class || type == Byte.class || type == Character.class ||
               type == Short.class || type == Integer.class || type == Long.class ||
               type == Float.class || type == Double.class || type == BigDecimal.class;
    }

    /**
     * 从数组中随机选择一个值
     */
    private String pickRandom(String... values) {
        return values[random.nextInt(values.length)];
    }

    /**
     * 四舍五入到指定小数位
     */
    private double round(double value, int places) {
        long factor = (long) Math.pow(10, places);
        return (double) Math.round(value * factor) / factor;
    }
}