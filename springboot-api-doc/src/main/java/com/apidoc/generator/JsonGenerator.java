package com.apidoc.generator;

import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 简单JSON序列化器
 * 避免依赖Jackson等外部库
 */
@Component
public class JsonGenerator {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 对象转JSON字符串
     */
    public String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        writeValue(sb, obj, 0);
        return sb.toString();
    }

    /**
     * 对象转格式化JSON字符串
     */
    public String toPrettyJson(Object obj) {
        if (obj == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        writeValue(sb, obj, 0, true);
        return sb.toString();
    }

    /**
     * 写入值
     */
    private void writeValue(StringBuilder sb, Object obj, int depth) {
        writeValue(sb, obj, depth, false);
    }

    /**
     * 写入值（支持格式化）
     */
    private void writeValue(StringBuilder sb, Object obj, int depth, boolean pretty) {
        if (obj == null) {
            sb.append("null");
            return;
        }

        Class<?> clazz = obj.getClass();

        // 字符串类型
        if (obj instanceof String) {
            sb.append("\"").append(escapeJson((String) obj)).append("\"");
        }
        // 数字类型
        else if (obj instanceof Number) {
            sb.append(obj.toString());
        }
        // 布尔类型
        else if (obj instanceof Boolean) {
            sb.append(obj.toString());
        }
        // 日期类型
        else if (obj instanceof Date) {
            sb.append("\"").append(((Date) obj).toString()).append("\"");
        }
        else if (obj instanceof LocalDateTime) {
            sb.append("\"").append(((LocalDateTime) obj).format(DATE_TIME_FORMATTER)).append("\"");
        }
        else if (obj instanceof LocalDate) {
            sb.append("\"").append(((LocalDate) obj).format(DATE_FORMATTER)).append("\"");
        }
        // 集合类型
        else if (obj instanceof Collection) {
            writeCollection(sb, (Collection<?>) obj, depth, pretty);
        }
        // Map类型
        else if (obj instanceof Map) {
            writeMap(sb, (Map<?, ?>) obj, depth, pretty);
        }
        // 数组类型
        else if (clazz.isArray()) {
            writeArray(sb, obj, depth, pretty);
        }
        // 普通对象
        else {
            writeObject(sb, obj, depth, pretty);
        }
    }

    /**
     * 写入集合
     */
    private void writeCollection(StringBuilder sb, Collection<?> collection, int depth, boolean pretty) {
        sb.append("[");

        if (pretty && !collection.isEmpty()) {
            sb.append("\n");
        }

        Iterator<?> iterator = collection.iterator();
        while (iterator.hasNext()) {
            if (pretty) {
                sb.append(getIndent(depth + 1));
            }

            writeValue(sb, iterator.next(), depth + 1, pretty);

            if (iterator.hasNext()) {
                sb.append(",");
            }

            if (pretty) {
                sb.append("\n");
            }
        }

        if (pretty && !collection.isEmpty()) {
            sb.append(getIndent(depth));
        }
        sb.append("]");
    }

    /**
     * 写入Map
     */
    private void writeMap(StringBuilder sb, Map<?, ?> map, int depth, boolean pretty) {
        sb.append("{");

        if (pretty && !map.isEmpty()) {
            sb.append("\n");
        }

        Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<?, ?> entry = iterator.next();

            if (pretty) {
                sb.append(getIndent(depth + 1));
            }

            // 写入键
            writeValue(sb, entry.getKey().toString(), depth + 1, pretty);
            sb.append(":");
            if (pretty) {
                sb.append(" ");
            }

            // 写入值
            writeValue(sb, entry.getValue(), depth + 1, pretty);

            if (iterator.hasNext()) {
                sb.append(",");
            }

            if (pretty) {
                sb.append("\n");
            }
        }

        if (pretty && !map.isEmpty()) {
            sb.append(getIndent(depth));
        }
        sb.append("}");
    }

    /**
     * 写入数组
     */
    private void writeArray(StringBuilder sb, Object array, int depth, boolean pretty) {
        sb.append("[");

        int length = java.lang.reflect.Array.getLength(array);
        if (pretty && length > 0) {
            sb.append("\n");
        }

        for (int i = 0; i < length; i++) {
            if (pretty) {
                sb.append(getIndent(depth + 1));
            }

            writeValue(sb, java.lang.reflect.Array.get(array, i), depth + 1, pretty);

            if (i < length - 1) {
                sb.append(",");
            }

            if (pretty) {
                sb.append("\n");
            }
        }

        if (pretty && length > 0) {
            sb.append(getIndent(depth));
        }
        sb.append("]");
    }

    /**
     * 写入普通对象
     */
    private void writeObject(StringBuilder sb, Object obj, int depth, boolean pretty) {
        sb.append("{");

        Field[] fields = obj.getClass().getDeclaredFields();
        List<Field> accessibleFields = new ArrayList<>();

        // 筛选可访问的非静态字段
        for (Field field : fields) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                accessibleFields.add(field);
            }
        }

        if (pretty && !accessibleFields.isEmpty()) {
            sb.append("\n");
        }

        for (int i = 0; i < accessibleFields.size(); i++) {
            Field field = accessibleFields.get(i);

            try {
                Object value = field.get(obj);

                if (pretty) {
                    sb.append(getIndent(depth + 1));
                }

                // 写入字段名
                sb.append("\"").append(field.getName()).append("\":");
                if (pretty) {
                    sb.append(" ");
                }

                // 写入字段值
                writeValue(sb, value, depth + 1, pretty);

                if (i < accessibleFields.size() - 1) {
                    sb.append(",");
                }

                if (pretty) {
                    sb.append("\n");
                }

            } catch (IllegalAccessException e) {
                // 跳过无法访问的字段
            }
        }

        if (pretty && !accessibleFields.isEmpty()) {
            sb.append(getIndent(depth));
        }
        sb.append("}");
    }

    /**
     * 转义JSON字符串
     */
    private String escapeJson(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * 获取缩进字符串
     */
    private String getIndent(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth * 2; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }
}