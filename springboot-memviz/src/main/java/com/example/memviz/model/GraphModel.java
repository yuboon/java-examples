package com.example.memviz.model;

import cn.hutool.core.util.RandomUtil;

import java.util.*;

public class GraphModel {
    public static class Node {
        public String id;        // objectId 或 class@id
        public String label;     // 类名(短)
        public String className; // 类名(全)
        public long shallowSize; // 浅表大小
        public long deepSize;    // 深度大小
        public String category;  // JDK/第三方/业务
        public int instanceCount; // 该类的实例总数
        public String formattedSize; // 格式化的浅表大小显示
        public String formattedDeepSize; // 格式化的深度大小显示
        public String packageName; // 包名
        public boolean isArray; // 是否为数组类型
        public String objectType; // 对象类型描述
        public List<FieldInfo> fields; // 对象的字段信息

        public Node(String id, String label, String className, long shallowSize, String category) {
            this.id = id;
            this.label = label;
            this.className = className;
            this.shallowSize = shallowSize;
            this.category = category;
            this.fields = new ArrayList<>();
        }
        
        // 增强构造函数
        public Node(String id, String label, String className, long shallowSize, String category,
                   int instanceCount, String formattedSize, String packageName, boolean isArray, String objectType,
                   long deepSize, String formattedDeepSize) {
            this.id = id;
            this.label = label;
            this.className = className;
            this.shallowSize = shallowSize;
            this.deepSize = deepSize;
            this.category = category;
            this.instanceCount = instanceCount;
            this.formattedSize = formattedSize;
            this.formattedDeepSize = formattedDeepSize;
            this.packageName = packageName;
            this.isArray = isArray;
            this.objectType = objectType;
            this.fields = new ArrayList<>();
        }
    }

    public static class Link {
        public String source;
        public String target;
        public String field;   // 通过哪个字段/元素引用

        public Link(String s, String t, String field) {
            this.source = s;
            this.target = t;
            this.field = field;
        }
    }
    
    // Top100类统计信息
    public static class TopClassStat {
        public String className;
        public String shortName;
        public String packageName;
        public String category;
        public int instanceCount; // 实例数量
        public long totalSize; // 该类所有实例的总内存（浅表大小）
        public String formattedTotalSize; // 格式化的总内存
        public long totalDeepSize; // 该类所有实例的总深度大小
        public String formattedTotalDeepSize; // 格式化的总深度大小
        public long avgSize; // 平均每个实例大小（浅表）
        public String formattedAvgSize; // 格式化的平均大小
        public long avgDeepSize; // 平均每个实例深度大小
        public String formattedAvgDeepSize; // 格式化的平均深度大小
        public int rank; // 排名
        public List<ClassInstance> topInstances; // 该类中内存占用最大的实例列表
        
        public TopClassStat(String className, String shortName, String packageName, String category,
                           int instanceCount, long totalSize, String formattedTotalSize,
                           long totalDeepSize, String formattedTotalDeepSize,
                           long avgSize, String formattedAvgSize, 
                           long avgDeepSize, String formattedAvgDeepSize,
                           int rank, List<ClassInstance> topInstances) {
            this.className = className;
            this.shortName = shortName;
            this.packageName = packageName;
            this.category = category;
            this.instanceCount = instanceCount;
            this.totalSize = totalSize;
            this.formattedTotalSize = formattedTotalSize;
            this.totalDeepSize = totalDeepSize;
            this.formattedTotalDeepSize = formattedTotalDeepSize;
            this.avgSize = avgSize;
            this.formattedAvgSize = formattedAvgSize;
            this.avgDeepSize = avgDeepSize;
            this.formattedAvgDeepSize = formattedAvgDeepSize;
            this.rank = rank;
            this.topInstances = topInstances != null ? topInstances : new ArrayList<>();
        }
    }
    
    // 类的实例信息
    public static class ClassInstance {
        public String id;
        public long size; // 浅表大小
        public String formattedSize; // 格式化的浅表大小
        public long retainedSize; // 深度大小（保留大小）
        public String formattedRetainedSize; // 格式化的深度大小
        public int rank; // 在该类中的排名
        public String packageName; // 包名
        public String objectType; // 对象类型
        public boolean isArray; // 是否数组
        public double sizePercentInClass; // 在该类中的内存占比
        public List<FieldInfo> fields; // 添加字段信息列表
        
        public ClassInstance(String id, long size, String formattedSize, 
                           long retainedSize, String formattedRetainedSize, int rank, 
                           String packageName, String objectType, boolean isArray, double sizePercentInClass) {
            this.id = id;
            this.size = size;
            this.formattedSize = formattedSize;
            this.retainedSize = retainedSize;
            this.formattedRetainedSize = formattedRetainedSize;
            this.rank = rank;
            this.packageName = packageName;
            this.objectType = objectType;
            this.isArray = isArray;
            this.sizePercentInClass = sizePercentInClass;
            this.fields = new ArrayList<>();
        }
    }

    public List<Node> nodes = new ArrayList<>();
    public List<Link> links = new ArrayList<>();
    public List<TopClassStat> top100Classes = new ArrayList<>(); // Top100类统计列表
    public int totalObjects; // 总对象数
    public long totalMemory; // 总内存占用
    public String formattedTotalMemory; // 格式化的总内存
    
    // 字段信息类，用于存储对象的属性信息
    public static class FieldInfo {
        public String name;        // 字段名称
        public String type;        // 字段类型
        public String value;       // 字段值（字符串表示）
        public long size;          // 字段浅表大小（字节）
        public String formattedSize; // 格式化的浅表大小
        public long retainedSize;  // 字段深度大小（保留大小）
        public String formattedRetainedSize; // 格式化的深度大小
        public double sizePercent;   // 在对象中的占比（基于浅表大小）
        public double retainedSizePercent; // 在对象中的深度大小占比
        public boolean isPrimitive;  // 是否为基本类型
        public boolean isReference;  // 是否为引用类型
        
        public FieldInfo(String name, String type, String value, long size, 
                        String formattedSize, long retainedSize, String formattedRetainedSize,
                        double sizePercent, double retainedSizePercent,
                        boolean isPrimitive, boolean isReference) {
            this.name = name;
            this.type = type;
            this.value = value;
            this.size = size;
            this.formattedSize = formattedSize;
            this.retainedSize = retainedSize;
            this.formattedRetainedSize = formattedRetainedSize;
            this.sizePercent = sizePercent;
            this.retainedSizePercent = retainedSizePercent;
            this.isPrimitive = isPrimitive;
            this.isReference = isReference;
        }
        
        // 获取字段浅表大小
        public long getSize() {
            return size;
        }
        
        // 获取字段深度大小
        public long getRetainedSize() {
            return retainedSize;
        }
    }
}