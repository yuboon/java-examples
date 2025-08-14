package com.example.memviz.service;

import com.example.memviz.model.GraphModel;
import org.netbeans.lib.profiler.heap.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Predicate;

@Service
public class HprofParseService {
    
    private static final Logger log = LoggerFactory.getLogger(HprofParseService.class);

    /**
     * 安全阈值：最多加载多少对象/边进入图（避免前端崩溃）
     * 图上显示Top100类，保持完整但可读
     */
    private static final int MAX_GRAPH_NODES = 100;  // 图上显示的类数
    private static final int MAX_COLLECTION_NODES = 2000; // 收集的节点数，用于统计
    private static final int MAX_LINKS = 200;  // 增加连线数以适应更多类
    
    /**
     * 性能优化参数
     */
    private static final int BATCH_SIZE = 1000;  // 批量处理大小
    private static final int LARGE_CLASS_THRESHOLD = 10000;  // 大类阈值

    public GraphModel parseToGraph(java.io.File hprofFile,
                                   Predicate<String> classNameFilter,
                                   boolean collapseCollections) throws Exception {

        log.info("开始解析HPROF文件: {}", hprofFile.getName());

        // 检查文件大小和可用内存
        long fileSize = hprofFile.length();
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long availableMemory = maxMemory - (totalMemory - freeMemory);
        
        log.info("HPROF文件大小: {}MB, 可用内存: {}MB",
            fileSize / 1024.0 / 1024.0, availableMemory / 1024.0 / 1024.0);
        
        // 如果文件太大，警告用户并尝试优化加载
        if (fileSize > availableMemory * 0.3) {
            log.warn("检测到大型HPROF文件，启用内存优化加载模式");
            
            // 强制垃圾回收，释放更多内存
            System.gc();
            Thread.sleep(100);
            System.gc();
        }

        // Create heap from HPROF file with optimized settings
        Heap heap = null;
        try {
            heap = HeapFactory.createHeap(hprofFile);
            log.info("HPROF文件加载完成");
        } catch (OutOfMemoryError e) {
            log.error("内存不足：HPROF文件过大");
            throw new Exception("HPROF文件过大，内存不足。请增加JVM内存参数(-Xmx)或使用较小的堆转储文件", e);
        }

        try {
            return parseHeapData(heap, classNameFilter, collapseCollections);
        } finally {
            // 在finally块中确保释放资源
            if (heap != null) {
                try {
                    heap = null;
                    System.gc();
                    Thread.sleep(100);
                    System.gc();
                    log.info("已在finally块中释放HPROF文件引用");
                } catch (InterruptedException e) {
                    log.warn("释放文件引用时中断: {}", e.getMessage());
                }
            }
        }
    }

    private GraphModel parseHeapData(Heap heap, Predicate<String> classNameFilter, boolean collapseCollections) {
        
        // 1) 收集对象（可按类名过滤）- 极速优化版本，带内存监控
        List<Instance> all = new ArrayList<>(MAX_COLLECTION_NODES * 2);  // 预分配适量容量
        
        log.info("开始收集对象实例，使用激进优化策略");
        long startTime = System.currentTimeMillis();
        int processedClasses = 0;
        int skippedEmptyClasses = 0;
        int memoryCheckCounter = 0;
        
        // 使用优先队列在收集过程中就维护Top对象，避免后期排序
        PriorityQueue<Instance> topInstances = new PriorityQueue<>(
            MAX_COLLECTION_NODES * 2, Comparator.comparingLong(Instance::getSize)
        );
        
        // 直接处理，不预扫描，使用更激进的策略
        for (JavaClass javaClass : heap.getAllClasses()) {
            String className = javaClass.getName();
            
            // 更严格的早期过滤 - 临时放宽过滤条件
            if (classNameFilter != null && !classNameFilter.test(className)) {
                // 为了调试，记录被过滤掉的重要类
                if (className.contains("MemvizApplication") || className.contains("String") || className.contains("byte")) {
                    log.info("类被过滤掉: {}", className);
                }
                continue;
            }
            
            // 跳过明显的系统类和空类（基于类名）- 暂时禁用以确保不漏掉重要对象
            /*if (isLikelySystemClass(className)) {
                continue;
            }*/
            
            // 记录被处理的类
            log.debug("处理类: {}", className);
            
            // 定期检查内存使用情况
            if (++memoryCheckCounter % 100 == 0) {
                long currentFree = Runtime.getRuntime().freeMemory();
                long currentTotal = Runtime.getRuntime().totalMemory();
                long usedMemory = currentTotal - currentFree;
                double usedPercent = (double) usedMemory / Runtime.getRuntime().maxMemory() * 100;
                
                if (usedPercent > 85) {
                    log.warn("内存使用率高: {:.1f}%, 执行垃圾回收", usedPercent);
                    System.gc();
                    
                    // 重新检查
                    currentFree = Runtime.getRuntime().freeMemory();
                    currentTotal = Runtime.getRuntime().totalMemory();
                    usedMemory = currentTotal - currentFree;
                    usedPercent = (double) usedMemory / Runtime.getRuntime().maxMemory() * 100;
                    
                    if (usedPercent > 90) {
                        log.error("内存使用率危险，提前停止收集");
                        break;
                    }
                }
            }
            
            long classStart = System.currentTimeMillis();
            
            try {
                // 直接获取实例，设置超时检查
                List<Instance> instances = javaClass.getInstances();
                int instanceCount = instances.size();
                
                if (instanceCount == 0) {
                    skippedEmptyClasses++;
                    continue;
                }
                
                // 智能采样：使用优先队列自动维护Top对象
                if (instanceCount > LARGE_CLASS_THRESHOLD) {
                    // 超大类：激进采样，直接加入优先队列
                    int sampleSize = Math.min(100, instanceCount / 10); 
                    int step = Math.max(1, instanceCount / sampleSize);
                    
                    for (int i = 0; i < instanceCount; i += step) {
                        Instance inst = instances.get(i);
                        addToTopInstances(topInstances, inst, MAX_GRAPH_NODES * 2);
                    }
                    log.debug("大类采样: {}, 采样数: {}", className, Math.min(sampleSize, instanceCount));
                } else {
                    // 小类：全部加入优先队列
                    for (Instance inst : instances) {
                        addToTopInstances(topInstances, inst, MAX_COLLECTION_NODES * 2);
                    }
                }
                
                // 处理完大量数据后，帮助GC回收临时对象
                if (instanceCount > 1000) {
                    instances = null; // 显式清除引用
                }
                
                processedClasses++;
                long classEnd = System.currentTimeMillis();
                
                // 只记录耗时较长的类
                if (classEnd - classStart > 100) {
                    log.debug("耗时类: {}, 实例数: {}, 耗时: {}ms, 总计: {}", 
                        className, instanceCount, (classEnd - classStart), all.size());
                }
                
                // 每处理一定数量的类就检查是否应该停止
                if (processedClasses % 50 == 0) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    if (elapsed > 30000) { // 30秒超时
                        log.warn("处理时间过长，停止收集");
                        break;
                    }
                    log.info("进度: {}个类, {}个实例, 耗时{}ms", processedClasses, all.size(), elapsed);
                }
                
            } catch (Exception e) {
                log.warn("处理类失败: {}, 错误: {}", className, e.getMessage());
                continue;
            }
        }

        // 从优先队列中提取所有结果用于统计
        List<Instance> allCollectedInstances = new ArrayList<>(topInstances);
        allCollectedInstances.sort(Comparator.comparingLong(Instance::getSize).reversed());
        
        // 图显示用的Top100对象
        List<Instance> graphInstances = new ArrayList<>();
        int graphNodeCount = Math.min(MAX_GRAPH_NODES, allCollectedInstances.size());
        for (int i = 0; i < graphNodeCount; i++) {
            graphInstances.add(allCollectedInstances.get(i));
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("收集完成: {}个类已处理, {}个空类跳过, {}个实例收集完成(图显示{}个), 耗时{}ms", 
            processedClasses, skippedEmptyClasses, allCollectedInstances.size(), graphInstances.size(), totalTime);
        
        log.info("图节点数量: {}, 统计节点数量: {}", graphInstances.size(), allCollectedInstances.size());

        // 3) 建立 id 映射，统计类型和数量信息，生成增强数据
        Map<Long, GraphModel.Node> nodeMap = new LinkedHashMap<>();
        Map<String, Integer> classCountMap = new HashMap<>();  // 统计每个类的实例数量
        GraphModel graph = new GraphModel();

        // 用所有收集的实例进行类统计（不仅仅是图显示的Top10）
        for (Instance obj : allCollectedInstances) {
            String cn = className(heap, obj);
            classCountMap.put(cn, classCountMap.getOrDefault(cn, 0) + 1);
        }

        // 计算总内存占用 - 使用原始数据而不是过滤后的数据
        long totalMemoryBeforeFilter = 0;
        int totalObjectsBeforeFilter = 0;
        
        // 统计所有对象（用于准确的总内存计算）
        for (JavaClass javaClass : heap.getAllClasses()) {
            String className = javaClass.getName();
            
            // 应用类名过滤器进行统计
            boolean passesFilter = (classNameFilter == null || classNameFilter.test(className));
            
            // 记录重要的类信息
            if (className.contains("MemvizApplication") || className.contains("GraphModel")) {
                log.info("发现重要类: {}, 通过过滤器: {}", className, passesFilter);
            }
            
            if(!passesFilter){
                continue;
            }

            // instances 前后加耗时日志统计
            long start = System.currentTimeMillis();
            List<Instance> instances = javaClass.getInstances();
            long end = System.currentTimeMillis();
            if ((end - start) > 50) { // 只记录耗时的调用
                log.info("获取类 {} 的实例耗时: {}ms, 实例数: {}", className, (end - start), instances.size());
            }
            
            for (Instance instance : instances) {
                totalObjectsBeforeFilter++;
                totalMemoryBeforeFilter += instance.getSize();
                
                // 记录大对象
                if (instance.getSize() > 500 * 1024) { // 大于500KB的对象
                    log.info("发现大对象: 类={}, 大小={}, ID={}", className, formatSize(instance.getSize()), instance.getInstanceId());
                }
            }
        }
        
        long instanceTotalMemory = allCollectedInstances.stream().mapToLong(Instance::getSize).sum();
        graph.totalObjects = totalObjectsBeforeFilter;  // 显示总对象数，而不是过滤后的
        graph.totalMemory = totalMemoryBeforeFilter;    // 显示总内存，而不是过滤后的
        graph.formattedTotalMemory = formatSize(totalMemoryBeforeFilter);
        
        log.info("内存统计: 总对象数={}, 总内存={}", graph.totalObjects, graph.formattedTotalMemory);
        log.info("收集对象数={}, 收集内存={}, 图中对象数={}, 图中内存={}", 
            allCollectedInstances.size(), formatSize(instanceTotalMemory),
            graphInstances.size(), formatSize(graphInstances.stream().mapToLong(Instance::getSize).sum()));

        // 直接从所有类创建Top100类统计列表（不依赖收集的实例，确保统计完整）
        List<GraphModel.TopClassStat> allClassStats = new ArrayList<>();
        
        for (JavaClass javaClass : heap.getAllClasses()) {
            String className = javaClass.getName();
            
            // 应用过滤条件
            if (classNameFilter != null && !classNameFilter.test(className)) {
                continue;
            }
            
            try {
                List<Instance> instances = javaClass.getInstances();
                int instanceCount = instances.size();
                
                // 跳过没有实例的类
                if (instanceCount == 0) {
                    continue;
                }
                
                // 跳过Lambda表达式生成的匿名类
                if (className.contains("$$Lambda") || className.contains("$Lambda")) {
                    continue;
                }
                
                // 跳过其他JVM生成的内部类
                if (className.contains("$$EnhancerBySpringCGLIB$$") || 
                    className.contains("$$FastClassBySpringCGLIB$$") ||
                    className.contains("$Proxy$")) {
                    continue;
                }

                // 删除测试代码，使用正式的API调用
                
                // 计算该类的总内存占用（浅表大小）
                long totalSize = instances.stream().mapToLong(Instance::getSize).sum();
                long avgSize = totalSize / instanceCount;
                
                // 使用NetBeans API计算深度大小（保留大小）
                long totalRetainedSize = calculateTotalRetainedSize(instances);
                long avgRetainedSize = totalRetainedSize / instanceCount;
                
                // 记录保留大小计算结果（特别是大差异的情况）
                if (totalRetainedSize > totalSize * 2) { // 保留大小是浅表大小的2倍以上
                    log.info("类 {} 保留大小显著大于浅表大小: 浅表={}({}) vs 保留={}({})", 
                        className, totalSize, formatSize(totalSize), 
                        totalRetainedSize, formatSize(totalRetainedSize));
                }
                
                String displayCategory = formatCategory(categoryOf(className));
                String packageName = extractPackageName(className);
                
                // 创建该类的Top实例列表（按深度大小排序，最多100个）
                List<Instance> sortedInstances = new ArrayList<>(instances);
                // 按深度大小（保留大小）排序，如果获取失败则使用浅表大小
                sortedInstances.sort((a, b) -> {
                    try {
                        long retainedA = a.getRetainedSize();
                        long retainedB = b.getRetainedSize();
                        // 确保深度大小至少等于浅表大小
                        retainedA = Math.max(retainedA, a.getSize());
                        retainedB = Math.max(retainedB, b.getSize());
                        return Long.compare(retainedB, retainedA); // 降序
                    } catch (Exception e) {
                        // 如果获取深度大小失败，回退到浅表大小比较
                        return Long.compare(b.getSize(), a.getSize());
                    }
                });
                
                List<GraphModel.ClassInstance> classInstances = new ArrayList<>();
                for (int i = 0; i < Math.min(100, sortedInstances.size()); i++) {
                    Instance inst = sortedInstances.get(i);
                    String instClassName = className(heap, inst);
                    String instPackageName = extractPackageName(instClassName);
                    String objectType = determineObjectType(instClassName);
                    boolean isArray = instClassName.contains("[");
                    
                    // 获取实例的深度大小
                    long instanceRetainedSize;
                    try {
                        instanceRetainedSize = inst.getRetainedSize();
                        // 确保深度大小至少等于浅表大小
                        instanceRetainedSize = Math.max(instanceRetainedSize, inst.getSize());
                    } catch (Exception e) {
                        log.debug("获取实例深度大小失败，使用浅表大小: {}", e.getMessage());
                        instanceRetainedSize = inst.getSize();
                    }
                    
                    // 计算该实例在该类中的内存占比（基于深度大小）
                    double sizePercent = totalRetainedSize > 0 ? (double) instanceRetainedSize / totalRetainedSize * 100.0 : 0.0;
                    
                    GraphModel.ClassInstance classInstance = new GraphModel.ClassInstance(
                        String.valueOf(inst.getInstanceId()),
                        inst.getSize(),
                        formatSize(inst.getSize()),
                        instanceRetainedSize,  // 添加深度大小
                        formatSize(instanceRetainedSize),  // 格式化的深度大小
                        i + 1,
                        instPackageName,
                        objectType,
                        isArray,
                        sizePercent
                    );
                    
                    // 添加对象属性分析
                    analyzeInstanceFields(inst, classInstance);
                    
                    // 注意：不再为每个实例创建单独的节点，而是在类级别聚合显示
                    // 实例详细信息保存在ClassInstance中，通过Top100类统计展示
                    
                    classInstances.add(classInstance);
                }
                
                GraphModel.TopClassStat stat = new GraphModel.TopClassStat(
                    className,
                    shortName(className),
                    packageName,
                    displayCategory,
                    instanceCount,
                    totalSize,
                    formatSize(totalSize),
                    totalRetainedSize,  // 使用保留大小
                    formatSize(totalRetainedSize),  // 格式化的保留大小
                    avgSize,
                    formatSize(avgSize),
                    avgRetainedSize,  // 平均保留大小
                    formatSize(avgRetainedSize),  // 格式化的平均保留大小
                    0,
                    classInstances
                );
                allClassStats.add(stat);
                
            } catch (Exception e) {
                log.warn("处理类{}时出错: {}", className, e.getMessage());
            }
        }
        
        // 按总深度大小排序并设置排名
        allClassStats.sort(Comparator.comparingLong((GraphModel.TopClassStat s) -> s.totalDeepSize).reversed());
        for (int i = 0; i < Math.min(100, allClassStats.size()); i++) {
            allClassStats.get(i).rank = i + 1;
            graph.top100Classes.add(allClassStats.get(i));
        }
        
        log.info("类统计完成: 共{}个类符合过滤条件，Top100类已生成", allClassStats.size());

        // 用Top100类统计数据创建图显示用的类节点
        // 按总深度大小排序，取Top100用于图显示
        List<GraphModel.TopClassStat> topClassesForGraph = new ArrayList<>(allClassStats);
        topClassesForGraph.sort(Comparator.comparingLong((GraphModel.TopClassStat s) -> s.totalDeepSize).reversed());
        
        // 为图显示的Top100类创建节点
        int graphClassCount = Math.min(MAX_GRAPH_NODES, topClassesForGraph.size());
        for (int i = 0; i < graphClassCount; i++) {
            GraphModel.TopClassStat classStat = topClassesForGraph.get(i);
            String cn = classStat.className;
            
            // 创建类级别的节点，显示类的聚合信息（包含深度大小）
            String enhancedLabel = String.format("%s (%d个实例, 浅表:%s, 深度:%s, %s)", 
                classStat.shortName, classStat.instanceCount, 
                classStat.formattedTotalSize, classStat.formattedTotalDeepSize, classStat.category);
            
            GraphModel.Node n = new GraphModel.Node(
                    "class_" + cn.hashCode(), // 使用类名hash作为节点ID
                    enhancedLabel,
                    cn,
                    classStat.totalSize,
                    classStat.category,
                    classStat.instanceCount,
                    classStat.formattedTotalSize,
                    classStat.packageName,
                    cn.contains("["),
                    determineObjectType(cn),
                    classStat.totalDeepSize,         // 深度大小
                    classStat.formattedTotalDeepSize // 格式化的深度大小
            );
            
            nodeMap.put((long)cn.hashCode(), n); // 用类名hash作为key
            graph.nodes.add(n);
        }

        // 4) 建立类级别的引用边（基于堆中真实的对象引用关系）
        log.info("开始建立类级别引用边，图类数: {}", graphClassCount);
        int linkCount = 0;
        int potentialLinks = 0;
        
        // 分析类之间的引用关系 - 只基于堆中真实的对象引用
        Map<String, Set<String>> classReferences = new HashMap<>();
        
        for (Instance obj : allCollectedInstances) {
            String sourceClassName = className(heap, obj);
            
            for (FieldValue fieldValue : obj.getFieldValues()) {
                potentialLinks++;
                if (fieldValue instanceof ObjectFieldValue) {
                    ObjectFieldValue objFieldValue = (ObjectFieldValue) fieldValue;
                    Instance target = objFieldValue.getInstance();
                    if (target != null) {
                        String targetClassName = className(heap, target);
                        
                        // 避免自引用，也避免Lambda和代理类的连线
                        if (!sourceClassName.equals(targetClassName) && 
                            !isGeneratedClass(targetClassName) && 
                            !isGeneratedClass(sourceClassName)) {
                            classReferences.computeIfAbsent(sourceClassName, k -> new HashSet<>())
                                          .add(targetClassName);
                        }
                    }
                }
            }
        }
        
        log.info("检测到类引用关系: {}", classReferences.size());
        
        // 为图中显示的类创建连线
        for (int i = 0; i < graphClassCount && linkCount < MAX_LINKS; i++) {
            String sourceClass = topClassesForGraph.get(i).className;
            Set<String> targets = classReferences.get(sourceClass);
            
            if (targets != null) {
                for (String targetClass : targets) {
                    // 检查目标类是否也在图显示范围内
                    boolean targetInGraph = topClassesForGraph.stream()
                        .limit(graphClassCount)
                        .anyMatch(stat -> stat.className.equals(targetClass));
                    
                    if (targetInGraph) {
                        String sourceId = "class_" + sourceClass.hashCode();
                        String targetId = "class_" + targetClass.hashCode();
                        
                        // 添加更详细的连线信息
                        String linkLabel = "引用";
                        graph.links.add(new GraphModel.Link(sourceId, targetId, linkLabel));
                        linkCount++;
                        
                        if (linkCount >= MAX_LINKS) {
                            log.info("达到最大连线数限制: {}", MAX_LINKS);
                            break;
                        }
                    }
                }
            }
        }
        
        log.info("连线建立完成: 处理了{}个潜在连线，实际创建{}个连线", potentialLinks, linkCount);

        // 5) 可选：把大型集合折叠为"聚合节点"，减少噪音
        if (collapseCollections) {
            log.info("开始折叠集合类型节点");
            collapseCollectionLikeNodes(graph);
        }

        log.info("图构建完成: {}个节点, {}个链接", graph.nodes.size(), graph.links.size());

        return graph;
    }

    private static String className(Heap heap, Instance instance) {
        return instance.getJavaClass().getName();
    }

    private static String shortName(String fqcn) {
        int p = fqcn.lastIndexOf('.');
        return p >= 0 ? fqcn.substring(p + 1) : fqcn;
    }

    private static String categoryOf(String fqcn) {
        if (fqcn.startsWith("java.") || fqcn.startsWith("javax.") || fqcn.startsWith("jdk.")) return "JDK";
        if (fqcn.startsWith("org.") || fqcn.startsWith("com.")) return "3rd";
        return "app";
    }

    /**
     * 格式化字节大小，让显示更直观
     */
    private static String formatSize(long sizeInBytes) {
        if (sizeInBytes < 1024) {
            return sizeInBytes + "B";
        } else if (sizeInBytes < 1024 * 1024) {
            return String.format("%.1fKB", sizeInBytes / 1024.0);
        } else if (sizeInBytes < 1024 * 1024 * 1024) {
            return String.format("%.2fMB", sizeInBytes / (1024.0 * 1024));
        } else {
            return String.format("%.2fGB", sizeInBytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 格式化类别名称，让显示更直观
     */
    private static String formatCategory(String category) {
        switch (category) {
            case "JDK":
                return "JDK类";
            case "3rd":
                return "第三方";
            case "app":
                return "业务代码";
            default:
                return "未知";
        }
    }

    /**
     * 提取包名
     */
    private static String extractPackageName(String className) {
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            return className.substring(0, lastDot);
        }
        return "默认包";
    }

    /**
     * 确定对象类型
     */
    private static String determineObjectType(String className) {
        if (className.contains("[")) {
            return "数组";
        } else if (className.contains("$")) {
            if (className.contains("Lambda")) {
                return "Lambda表达式";
            } else {
                return "内部类";
            }
        } else if (className.startsWith("java.util.") && 
                  (className.contains("List") || className.contains("Set") || className.contains("Map"))) {
            return "集合类";
        } else if (className.startsWith("java.lang.")) {
            return "基础类型";
        } else {
            return "普通类";
        }
    }

    /**
     * 向优先队列添加实例，自动维护Top-N
     */
    private void addToTopInstances(PriorityQueue<Instance> topInstances, Instance instance, int maxSize) {
        if (topInstances.size() < maxSize) {
            topInstances.offer(instance);
        } else if (instance.getSize() > topInstances.peek().getSize()) {
            topInstances.poll();
            topInstances.offer(instance);
        }
    }

    /**
     * 快速选择Top-N最大的对象，避免全排序的性能问题
     */
    private List<Instance> quickSelectTopN(List<Instance> instances, int n) {
        if (instances.size() <= n) {
            return instances;
        }
        
        // 使用优先队列（小顶堆）来维护Top-N
        PriorityQueue<Instance> topN = new PriorityQueue<>(
            Comparator.comparingLong(Instance::getSize)
        );
        
        int processed = 0;
        for (Instance instance : instances) {
            if (topN.size() < n) {
                topN.offer(instance);
            } else if (instance.getSize() > topN.peek().getSize()) {
                topN.poll();
                topN.offer(instance);
            }
            
            // 每处理10000个对象记录一次进度
            if (++processed % 10000 == 0) {
                log.debug("快速选择进度: {}/{}", processed, instances.size());
            }
        }
        
        // 将结果转换为List并按大小降序排序
        List<Instance> result = new ArrayList<>(topN);
        result.sort(Comparator.comparingLong(Instance::getSize).reversed());
        
        log.info("快速选择完成，从{}个对象中选出{}个最大对象", instances.size(), result.size());
        return result;
    }
    private static boolean isLikelySystemClass(String className) {
        // 跳过一些已知很慢或不重要的类
        return className.startsWith("java.lang.Class") ||
               className.startsWith("java.lang.String") ||
               className.startsWith("java.lang.Object[]") ||
               className.startsWith("java.util.concurrent") ||
               className.contains("$$Lambda") ||
               className.contains("$Proxy") ||
               className.startsWith("sun.") ||
               className.startsWith("jdk.internal.") ||
               className.endsWith("[][]") || // 多维数组通常很慢
               className.contains("reflect.Method") ||
               className.contains("reflect.Field");
        //return false;
    }
    
    // 删除重复定义的 formatSize 方法，在其他地方已定义
    
    /**
     * 获取基本类型的大小
     */
    private long getPrimitiveTypeSize(String type) {
        switch (type) {
            case "boolean":
            case "byte":
                return 1;
            case "char":
            case "short":
                return 2;
            case "int":
            case "float":
                return 4;
            case "long":
            case "double":
                return 8;
            default:
                return 4; // 默认值
        }
    }
    
    /**
     * 获取字段的大小
     */
    private long getFieldSize(FieldValue fieldValue) {
        if (fieldValue instanceof ObjectFieldValue) {
            // 对于对象引用字段，返回引用本身的大小（指针大小），而不是被引用对象的大小
            // 在64位JVM中，对象引用通常是8字节；在32位JVM中通常是4字节
            // 这里假设是64位JVM
            return 8; // 对象引用的大小
        } else {
            // 处理非对象字段（基本类型）
            return getPrimitiveTypeSize(fieldValue.getField().getType().getName());
        }
    }
    
    /**
     * 分析实例的字段信息，添加到Node对象中
     */
    private void analyzeInstanceFieldsForNode(Instance instance, GraphModel.Node node) {
        try {
            // 获取实例的所有字段值
            List<FieldValue> fieldValues = instance.getFieldValues();
            if (fieldValues == null || fieldValues.isEmpty()) {
                // 处理数组类型
                if (instance instanceof ObjectArrayInstance) {
                    analyzeArrayInstanceForNode((ObjectArrayInstance) instance, node);
                } else if (instance instanceof PrimitiveArrayInstance) {
                    analyzePrimitiveArrayInstanceForNode((PrimitiveArrayInstance) instance, node);
                }
                return;
            }
            
            // 计算所有字段的总大小（用于计算占比）
            long totalObjectSize = instance.getSize();
            
            // 计算所有字段的实际大小之和
            long actualFieldsSize = fieldValues.stream().mapToLong(this::getFieldSize).sum();
            
            // 计算对象头部大小（对象总大小 - 字段大小）
            long objectHeaderSize = totalObjectSize - actualFieldsSize;
            
            // 按字段大小排序（从大到小）
            fieldValues.sort((f1, f2) -> {
                long size1 = getFieldSize(f1);
                long size2 = getFieldSize(f2);
                return Long.compare(size2, size1);
            });
            
            // 添加对象头部信息作为第一个字段
            // 注意：如果字段大小之和超过对象总大小，说明存在重复计算或其他问题
            if (objectHeaderSize > 0) {
                double headerPercent = totalObjectSize > 0 ? (double) objectHeaderSize / totalObjectSize * 100.0 : 0.0;
                GraphModel.FieldInfo headerInfo = new GraphModel.FieldInfo(
                    "<object header>",
                    "Object Header",
                    "对象头部信息（类指针、标记字等）",
                    objectHeaderSize,
                    formatSize(objectHeaderSize),
                    objectHeaderSize, // 深度大小等于浅表大小
                    formatSize(objectHeaderSize),
                    headerPercent,
                    headerPercent, // 深度大小占比等于浅表大小占比
                    false,
                    false
                );
                node.fields.add(headerInfo);
            } else if (objectHeaderSize < 0) {
                // 如果对象头部大小为负数，说明字段大小之和超过了对象总大小
                // 这种情况下，我们显示一个警告信息
                GraphModel.FieldInfo warningInfo = new GraphModel.FieldInfo(
                    "<size warning>",
                    "Warning",
                    "字段大小之和(" + formatSize(actualFieldsSize) + ")超过对象总大小(" + formatSize(totalObjectSize) + ")",
                    0,
                    "0B",
                    0,
                    "0B",
                    0.0,
                    0.0,
                    false,
                    false
                );
                node.fields.add(warningInfo);
            }
            
            // 分析每个字段
            for (FieldValue fieldValue : fieldValues) {
                String fieldName = fieldValue.getField().getName();
                String fieldType = fieldValue.getField().getType().getName();
                // 判断是否为基本类型
                boolean isPrimitive = isPrimitiveType(fieldValue.getField().getType().getName());
                boolean isReference = !isPrimitive;
                
                // 获取字段值和大小
                String valueStr = "";
                long fieldSize = getFieldSize(fieldValue); // 使用统一的字段大小计算方法
                
                if (fieldValue instanceof ObjectFieldValue) {
                    ObjectFieldValue objField = (ObjectFieldValue) fieldValue;
                    Instance fieldInstance = objField.getInstance();
                    
                    if (fieldInstance != null) {
                        valueStr = fieldInstance.getJavaClass().getName() + "@" + fieldInstance.getInstanceId();
                    } else {
                        valueStr = "null";
                        fieldSize = 0; // null引用的大小为0
                    }
                } else {
                    // 处理非对象字段（基本类型）
                    valueStr = String.valueOf(fieldValue.getValue());
                }
                
                // 计算字段在对象中的占比
                double sizePercent = totalObjectSize > 0 ? (double) fieldSize / totalObjectSize * 100.0 : 0.0;
                
                // 对于Node的字段分析，深度大小等于浅表大小（简化处理）
                long fieldRetainedSize = fieldSize;
                double retainedSizePercent = sizePercent;
                
                // 创建字段信息对象
                GraphModel.FieldInfo fieldInfo = new GraphModel.FieldInfo(
                    fieldName,
                    fieldType,
                    valueStr,
                    fieldSize,
                    formatSize(fieldSize),
                    fieldRetainedSize,
                    formatSize(fieldRetainedSize),
                    sizePercent,
                    retainedSizePercent,
                    isPrimitive,
                    isReference
                );
                
                // 添加到节点的字段列表
                node.fields.add(fieldInfo);
            }
            
            // 不重新排序字段，保持对象头部信息在最前面
            // 字段已经在添加前按大小排序过了
            
        } catch (Exception e) {
            log.warn("分析实例字段失败: {}, 错误: {}", instance.getJavaClass().getName(), e.getMessage());
        }
    }
    
    /**
     * 分析对象数组实例并添加到Node对象中
     */
    private void analyzeArrayInstanceForNode(ObjectArrayInstance arrayInstance, GraphModel.Node node) {
        try {
            int length = arrayInstance.getLength();
            long totalSize = arrayInstance.getSize();
            
            // 获取数组元素类型
            String elementType = arrayInstance.getJavaClass().getName();
            if (elementType.startsWith("[")) {
                elementType = elementType.substring(1); // 去掉前面的'['
            }
            
            // 创建数组概览字段
            GraphModel.FieldInfo arrayInfo = new GraphModel.FieldInfo(
                "array",
                elementType + "[]",
                "长度: " + length,
                totalSize,
                formatSize(totalSize),
                totalSize, // 深度大小等于浅表大小
                formatSize(totalSize),
                1.0, // 100%
                1.0, // 深度大小占比也是100%
                false,
                true
            );
            node.fields.add(arrayInfo);
            
            // 分析前10个元素（或更少）
            int elementsToShow = Math.min(length, 10);
            for (int i = 0; i < elementsToShow; i++) {
                Instance element = arrayInstance.getValues().get(i);
                String valueStr = element != null ? 
                    element.getJavaClass().getName() + "@" + element.getInstanceId() : "null";
                long elementSize = element != null ? element.getSize() : 0;
                
                double elementPercent = totalSize > 0 ? (double) elementSize / totalSize : 0.0;
                GraphModel.FieldInfo elementInfo = new GraphModel.FieldInfo(
                    "[" + i + "]",
                    elementType,
                    valueStr,
                    elementSize,
                    formatSize(elementSize),
                    elementSize, // 深度大小等于浅表大小
                    formatSize(elementSize),
                    elementPercent,
                    elementPercent, // 深度大小占比等于浅表大小占比
                    false,
                    true
                );
                node.fields.add(elementInfo);
            }
            
            // 如果数组很长，添加一个表示剩余元素的条目
            if (length > 10) {
                GraphModel.FieldInfo remainingInfo = new GraphModel.FieldInfo(
                    "[...]",
                    elementType,
                    "剩余 " + (length - 10) + " 个元素",
                    0,
                    "N/A",
                    0, // 深度大小
                    "N/A",
                    0.0,
                    0.0, // 深度大小占比
                    false,
                    true
                );
                node.fields.add(remainingInfo);
            }
        } catch (Exception e) {
            log.warn("分析数组实例失败: {}, 错误: {}", arrayInstance.getJavaClass().getName(), e.getMessage());
        }
    }
    
    /**
     * 分析基本类型数组实例并添加到Node对象中
     */
    private void analyzePrimitiveArrayInstanceForNode(PrimitiveArrayInstance arrayInstance, GraphModel.Node node) {
        try {
            int length = arrayInstance.getLength();
            long totalSize = arrayInstance.getSize();
            String elementType = arrayInstance.getJavaClass().getName();
            
            // 创建数组概览字段
            GraphModel.FieldInfo arrayInfo = new GraphModel.FieldInfo(
                "array",
                elementType,
                "长度: " + length,
                totalSize,
                formatSize(totalSize),
                totalSize, // 深度大小等于浅表大小
                formatSize(totalSize),
                1.0, // 100%
                1.0, // 深度大小占比也是100%
                true,
                false
            );
            node.fields.add(arrayInfo);
            
            // 对于基本类型数组，只显示长度和总大小信息
            // 如果是字符数组，可以尝试显示内容
            if (elementType.equals("char[]") && length <= 100) {
                try {
                    // 获取字符数组内容
                    char[] chars = new char[length];
                    List<Object> values = arrayInstance.getValues();
                    for (int i = 0; i < length; i++) {
                        Object value = values.get(i);
                        if (value instanceof Character) {
                            chars[i] = (Character) value;
                        }
                    }
                    String content = new String(chars);
                    if (content.length() > 50) {
                        content = content.substring(0, 47) + "...";
                    }
                    
                    GraphModel.FieldInfo contentInfo = new GraphModel.FieldInfo(
                        "content",
                        "String",
                        content,
                        0,
                        "N/A",
                        0, // 深度大小
                        "N/A",
                        0.0,
                        0.0, // 深度大小占比
                        true,
                        false
                    );
                    node.fields.add(contentInfo);
                } catch (Exception e) {
                    // 忽略字符数组内容获取错误
                }
            }
        } catch (Exception e) {
            log.warn("分析基本类型数组失败: {}, 错误: {}", arrayInstance.getJavaClass().getName(), e.getMessage());
        }
    }
    
    /**
     * 分析实例的字段信息，添加到ClassInstance对象中
     */
    private void analyzeInstanceFields(Instance instance, GraphModel.ClassInstance classInstance) {
        try {
            // 获取实例的所有字段值
            List<FieldValue> fieldValues = instance.getFieldValues();
            if (fieldValues == null || fieldValues.isEmpty()) {
                // 处理数组类型
                if (instance instanceof ObjectArrayInstance) {
                    analyzeArrayInstance((ObjectArrayInstance) instance, classInstance);
                } else if (instance instanceof PrimitiveArrayInstance) {
                    analyzePrimitiveArrayInstance((PrimitiveArrayInstance) instance, classInstance);
                }
                return;
            }
            
            // 计算所有字段的总大小（用于计算占比）
            long totalObjectSize = instance.getSize();
            long totalRetainedSize = 0;
            try {
                totalRetainedSize = instance.getRetainedSize();
                // 确保深度大小至少等于浅表大小
                if (totalRetainedSize < totalObjectSize) {
                    totalRetainedSize = totalObjectSize;
                }
            } catch (Exception e) {
                log.debug("获取实例深度大小失败，使用浅表大小: {}", instance.getJavaClass().getName());
                totalRetainedSize = totalObjectSize;
            }
            
            // 计算所有字段的实际大小之和
            long actualFieldsSize = fieldValues.stream().mapToLong(this::getFieldSize).sum();
            
            // 计算对象头部大小（对象总大小 - 字段大小）
            long objectHeaderSize = totalObjectSize - actualFieldsSize;
            
            // 按字段大小排序（从大到小）
            fieldValues.sort((f1, f2) -> {
                long size1 = getFieldSize(f1);
                long size2 = getFieldSize(f2);
                return Long.compare(size2, size1);
            });
            
            // 添加对象头部信息作为第一个字段
            if (objectHeaderSize > 0) {
                double headerPercent = totalObjectSize > 0 ? (double) objectHeaderSize / totalObjectSize * 100.0 : 0.0;
                double headerRetainedPercent = totalRetainedSize > 0 ? (double) objectHeaderSize / totalRetainedSize * 100.0 : 0.0;
                GraphModel.FieldInfo headerInfo = new GraphModel.FieldInfo(
                    "<object header>",
                    "Object Header",
                    "对象头部信息（类指针、标记字等）",
                    objectHeaderSize,
                    formatSize(objectHeaderSize),
                    objectHeaderSize, // 深度大小等于浅表大小
                    formatSize(objectHeaderSize),
                    headerPercent,
                    headerRetainedPercent,
                    false,
                    false
                );
                classInstance.fields.add(headerInfo);
            }
            
            // 分析每个字段
            for (FieldValue fieldValue : fieldValues) {
                String fieldName = fieldValue.getField().getName();
                String fieldType = fieldValue.getField().getType().getName();
                boolean isPrimitive = isPrimitiveType(fieldType);
                boolean isReference = !isPrimitive;
                
                // 获取字段值和大小
                String valueStr = "";
                long fieldSize = getFieldSize(fieldValue); // 使用统一的字段大小计算方法
                long fieldRetainedSize = 0;
                
                if (fieldValue instanceof ObjectFieldValue) {
                    ObjectFieldValue objField = (ObjectFieldValue) fieldValue;
                    Instance fieldInstance = objField.getInstance();
                    
                    if (fieldInstance != null) {
                        // 计算字段的深度大小
                        try {
                            fieldRetainedSize = fieldInstance.getRetainedSize();
                            // 确保深度大小至少等于浅表大小
                            if (fieldRetainedSize < fieldSize) {
                                fieldRetainedSize = fieldSize;
                            }
                        } catch (Exception e) {
                            log.debug("获取字段深度大小失败: {}, 使用浅表大小", fieldName);
                            fieldRetainedSize = fieldSize;
                        }
                        valueStr = fieldInstance.getJavaClass().getName() + "@" + fieldInstance.getInstanceId();
                    } else {
                        valueStr = "null";
                        fieldSize = 0; // null引用的大小为0
                        fieldRetainedSize = 0;
                    }
                } else {
                    // 处理非对象字段（基本类型）
                    valueStr = String.valueOf(fieldValue.getValue());
                    fieldRetainedSize = fieldSize; // 基本类型的深度大小等于浅表大小
                }
                
                // 计算字段在对象中的占比
                double sizePercent = totalObjectSize > 0 ? (double) fieldSize / totalObjectSize * 100.0 : 0.0;
                double retainedSizePercent = totalRetainedSize > 0 ? (double) fieldRetainedSize / totalRetainedSize * 100.0 : 0.0;
                
                // 创建字段信息对象
                GraphModel.FieldInfo fieldInfo = new GraphModel.FieldInfo(
                    fieldName,
                    fieldType,
                    valueStr,
                    fieldSize,
                    formatSize(fieldSize),
                    fieldRetainedSize,
                    formatSize(fieldRetainedSize),
                    sizePercent,
                    retainedSizePercent,
                    isPrimitive,
                    isReference
                );
                
                // 添加到实例的字段列表
                classInstance.fields.add(fieldInfo);
            }
            
            // 不重新排序字段，保持对象头部信息在最前面
            // 字段已经在添加前按大小排序过了
            
        } catch (Exception e) {
            log.warn("分析实例字段失败: {}, 错误: {}", instance.getJavaClass().getName(), e.getMessage());
        }
    }
    
    /**
     * 分析对象数组实例
     */
    private void analyzeArrayInstance(ObjectArrayInstance arrayInstance, GraphModel.ClassInstance classInstance) {
        try {
            int length = arrayInstance.getLength();
            long totalSize = arrayInstance.getSize();
            
            // 获取数组元素类型
            String elementType = arrayInstance.getJavaClass().getName();
            if (elementType.startsWith("[")) {
                elementType = elementType.substring(1); // 去掉前面的'['
            }
            
            // 创建数组概览字段
            double lengthPercent = (double) 4 / totalSize * 100.0;
            GraphModel.FieldInfo arrayInfo = new GraphModel.FieldInfo(
                "length",
                "int",
                String.valueOf(length),
                4, // int类型大小
                "4B",
                4, // 深度大小等于浅表大小
                "4B",
                lengthPercent,
                lengthPercent, // 深度大小占比等于浅表大小占比
                true,
                false
            );
            classInstance.fields.add(arrayInfo);
            
            // 分析数组元素（最多10个）
            int maxElements = Math.min(10, length);
            List<Instance> elements = arrayInstance.getValues();
            
            for (int i = 0; i < maxElements; i++) {
                Instance element = elements.get(i);
                if (element == null) continue;
                
                long elementSize = element.getSize();
                String elementValue = element.getJavaClass().getName() + "@" + element.getInstanceId();
                
                double elementPercent = (double) elementSize / totalSize * 100.0;
                GraphModel.FieldInfo elementInfo = new GraphModel.FieldInfo(
                    "[" + i + "]",
                    elementType,
                    elementValue,
                    elementSize,
                    formatSize(elementSize),
                    elementSize, // 深度大小等于浅表大小
                    formatSize(elementSize),
                    elementPercent,
                    elementPercent, // 深度大小占比等于浅表大小占比
                    false,
                    true
                );
                classInstance.fields.add(elementInfo);
            }
            
            // 如果数组元素很多，添加一个摘要信息
            if (length > 10) {
                long summarySize = totalSize - 4; // 减去length字段的大小
                double summaryPercent = (double) summarySize / totalSize * 100.0;
                GraphModel.FieldInfo summaryInfo = new GraphModel.FieldInfo(
                    "...",
                    elementType,
                    "还有" + (length - 10) + "个元素",
                    summarySize,
                    formatSize(summarySize),
                    summarySize, // 深度大小等于浅表大小
                    formatSize(summarySize),
                    summaryPercent,
                    summaryPercent, // 深度大小占比等于浅表大小占比
                    false,
                    true
                );
                classInstance.fields.add(summaryInfo);
            }
            
        } catch (Exception e) {
            log.warn("分析数组实例失败: {}, 错误: {}", arrayInstance.getJavaClass().getName(), e.getMessage());
        }
    }
    
    /**
     * 分析基本类型数组实例
     */
    private void analyzePrimitiveArrayInstance(PrimitiveArrayInstance arrayInstance, GraphModel.ClassInstance classInstance) {
        try {
            int length = arrayInstance.getLength();
            long totalSize = arrayInstance.getSize();
            String arrayType = arrayInstance.getJavaClass().getName();
            
            // 获取元素类型
            String elementType = "";
            int elementSize = 0;
            
            if (arrayType.equals("[Z")) { elementType = "boolean"; elementSize = 1; }
            else if (arrayType.equals("[B")) { elementType = "byte"; elementSize = 1; }
            else if (arrayType.equals("[C")) { elementType = "char"; elementSize = 2; }
            else if (arrayType.equals("[S")) { elementType = "short"; elementSize = 2; }
            else if (arrayType.equals("[I")) { elementType = "int"; elementSize = 4; }
            else if (arrayType.equals("[J")) { elementType = "long"; elementSize = 8; }
            else if (arrayType.equals("[F")) { elementType = "float"; elementSize = 4; }
            else if (arrayType.equals("[D")) { elementType = "double"; elementSize = 8; }
            
            // 创建数组概览字段
            double lengthPercent = (double) 4 / totalSize * 100.0;
            GraphModel.FieldInfo arrayInfo = new GraphModel.FieldInfo(
                "length",
                "int",
                String.valueOf(length),
                4, // int类型大小
                "4B",
                4L, // 深度大小等于浅表大小
                "4B",
                lengthPercent,
                lengthPercent, // 深度大小占比等于浅表大小占比
                true,
                false
            );
            classInstance.fields.add(arrayInfo);
            
            // 添加数组元素总大小信息
            long elementsSize = length * elementSize;
            double elementsPercent = (double) elementsSize / totalSize * 100.0;
            GraphModel.FieldInfo elementsInfo = new GraphModel.FieldInfo(
                "elements",
                elementType + "[]",
                length + "个" + elementType + "元素",
                elementsSize,
                formatSize(elementsSize),
                elementsSize, // 深度大小等于浅表大小
                formatSize(elementsSize),
                elementsPercent,
                elementsPercent, // 深度大小占比等于浅表大小占比
                true,
                false
            );
            classInstance.fields.add(elementsInfo);
            
            // 对于byte[]数组，尝试显示前20个字节的内容
            if (arrayType.equals("[B") && length > 0) {
                List<String> values = new ArrayList<>();
                int maxShow = Math.min(20, length);
                // 获取字节数组内容
                byte[] bytes = new byte[maxShow];
                try {
                    // 尝试通过反射获取数组内容
                    for (int i = 0; i < maxShow; i++) {
                        Object value = arrayInstance.getValues().get(i);
                        if (value instanceof Byte) {
                            bytes[i] = (Byte) value;
                        }
                    }
                } catch (Exception e) {
                    log.warn("获取字节数组内容失败: {}", e.getMessage());
                }
                
                for (int i = 0; i < maxShow; i++) {
                    values.add(String.format("%02X", bytes[i]));
                }
                
                String preview = String.join(" ", values) + (length > 20 ? "..." : "");
                
                GraphModel.FieldInfo previewInfo = new GraphModel.FieldInfo(
                    "preview",
                    "hex",
                    preview,
                    0, // 不计入大小
                    "0B",
                    0L, // 深度大小等于浅表大小
                    "0B",
                    0.0,
                    0.0, // 深度大小占比等于浅表大小占比
                    true,
                    false
                );
                classInstance.fields.add(previewInfo);
            }
            
        } catch (Exception e) {
            log.warn("分析基本类型数组失败: {}, 错误: {}", arrayInstance.getJavaClass().getName(), e.getMessage());
        }
    }
    
    // 已在上面定义了 getFieldSize 方法，这里删除重复定义
    
    /**
     * 判断是否为基本类型
     */
    
    /**
     * 判断是否为基本类型
     */
    private boolean isPrimitiveType(String typeName) {
        return typeName.equals("boolean") || 
               typeName.equals("byte") || 
               typeName.equals("char") || 
               typeName.equals("short") || 
               typeName.equals("int") || 
               typeName.equals("long") || 
               typeName.equals("float") || 
               typeName.equals("double");
    }
    
    // 已在上面定义了 getPrimitiveTypeSize 方法，这里删除重复定义

    /**
     * 确定类的分类
     */
    private String determineCategory(String className) {
        if (className.startsWith("java.lang.")) return "Java Lang";
        if (className.startsWith("java.util.")) return "Java Util";
        if (className.startsWith("java.io.")) return "Java IO";
        if (className.startsWith("java.net.")) return "Java Net";
        if (className.startsWith("java.sql.")) return "Java SQL";
        if (className.startsWith("javax.")) return "Java EE";
        if (className.startsWith("org.springframework.")) return "Spring";
        if (className.startsWith("com.example.")) return "Application";
        if (className.contains("[].")) return "Array";
        return "Other";
    }

    /**
     * 集合折叠策略：将集合类型的多个元素聚合显示
     */
    private void collapseCollectionLikeNodes(GraphModel graph) {
        Map<String, Integer> collectionElementCount = new HashMap<>();
        Set<String> collectionNodeIds = new HashSet<>();
        Set<GraphModel.Link> linksToRemove = new HashSet<>();
        Map<String, GraphModel.Link> collectionLinks = new HashMap<>();
        
        // 1. 识别集合类型的节点
        for (GraphModel.Node node : graph.nodes) {
            if (isCollectionType(node.className)) {
                collectionNodeIds.add(node.id);
            }
        }
        
        // 2. 统计每个集合的元素数量，并准备聚合连线
        for (GraphModel.Link link : graph.links) {
            if (collectionNodeIds.contains(link.source)) {
                // 这是从集合指向元素的连线
                String collectionId = link.source;
                collectionElementCount.put(collectionId, 
                    collectionElementCount.getOrDefault(collectionId, 0) + 1);
                linksToRemove.add(link);
                
                // 保留一条代表性连线，用于显示聚合信息
                String key = collectionId + "->elements";
                if (!collectionLinks.containsKey(key)) {
                    GraphModel.Node sourceNode = graph.nodes.stream()
                        .filter(n -> n.id.equals(collectionId))
                        .findFirst().orElse(null);
                    if (sourceNode != null) {
                        collectionLinks.put(key, new GraphModel.Link(
                            collectionId, 
                            "collapsed_" + collectionId, 
                            collectionElementCount.get(collectionId) + "个元素"
                        ));
                    }
                }
            }
        }
        
        // 3. 移除原始的集合元素连线
        graph.links.removeAll(linksToRemove);
        
        // 4. 更新集合节点的显示信息
        for (GraphModel.Node node : graph.nodes) {
            if (collectionNodeIds.contains(node.id)) {
                int elementCount = collectionElementCount.getOrDefault(node.id, 0);
                if (elementCount > 0) {
                    // 更新节点标签，显示元素数量
                    String originalLabel = node.label;
                    node.label = String.format("%s [%d个元素]", 
                        originalLabel.split("\\(")[0].trim(), elementCount);
                    
                    // 添加聚合信息到对象类型
                    node.objectType = node.objectType + " (已折叠)";
                }
            }
        }
        
        // 5. 移除被折叠的元素节点（可选，这里保留以维持图的完整性）
        // 实际应用中可以选择性移除孤立的元素节点
        
        log.info("集合折叠完成: {}个集合被处理", collectionElementCount.size());
    }
    
    /**
     * 使用NetBeans API计算总保留大小（更准确、更高效）
     */
    private long calculateTotalRetainedSize(List<Instance> instances) {
        long totalRetainedSize = 0;
        long totalShallowSize = 0;
        int retainedSizeFailures = 0;
        int retainedSizeSuccesses = 0;
        
        // 特殊分析：记录保留大小比浅表大小小的情况
        int retainedSmallerCount = 0;
        long retainedSmallerTotalShallow = 0;
        long retainedSmallerTotalRetained = 0;
        
        for (Instance instance : instances) {
            long shallowSize = instance.getSize();
            totalShallowSize += shallowSize;
            
            try {
                // 使用NetBeans API计算保留大小
                long retainedSize = instance.getRetainedSize();
                
                // 修复：确保保留大小至少等于浅表大小
                // 在某些情况下，由于共享引用，保留大小可能小于浅表大小
                // 但从用户角度看，对象的深度大小至少应该等于其浅表大小
                if (retainedSize < shallowSize) {
                    retainedSmallerCount++;
                    retainedSmallerTotalShallow += shallowSize;
                    retainedSmallerTotalRetained += retainedSize;
                    
                    // 分析引用情况
                    int incomingReferences = instance.getReferences().size();
                    log.debug("保留<浅表: 类={}, 浅表={}, 保留={}, 引用数={}", 
                        instance.getJavaClass().getName(),
                        formatSize(shallowSize), formatSize(retainedSize), incomingReferences);
                    
                    // 修复：使用浅表大小作为最小值
                    retainedSize = shallowSize;
                }
                
                totalRetainedSize += retainedSize;
                retainedSizeSuccesses++;
                
                // 记录特别大的对象或有显著差异的对象
                if (retainedSize > 1024 * 1024 || retainedSize > shallowSize * 2) {
                    log.info("对象保留大小: 类={}, 浅表={}({}), 保留={}({}), 差异={}倍", 
                        instance.getJavaClass().getName(),
                        shallowSize, formatSize(shallowSize),
                        retainedSize, formatSize(retainedSize),
                        String.format("%.2f", (double)retainedSize / shallowSize));
                }
            } catch (Exception e) {
                log.warn("获取保留大小失败: 类={}, 错误={}, 使用浅表大小代替", 
                    instance.getJavaClass().getName(), e.getMessage());
                totalRetainedSize += shallowSize; // 回退到浅表大小
                retainedSizeFailures++;
            }
        }
        
        // 如果有大量保留大小<浅表大小的情况，详细报告
        if (retainedSmallerCount > 0) {
            log.info("保留大小异常分析: {}个对象保留<浅表, 浅表总计={}, 保留总计={}, 已修正为使用浅表大小", 
                retainedSmallerCount, 
                formatSize(retainedSmallerTotalShallow), 
                formatSize(retainedSmallerTotalRetained));
        }
        
        log.info("保留大小计算统计: 成功={}, 失败={}, 浅表总计={}({}), 保留总计={}({}), 比率={}%", 
            retainedSizeSuccesses, retainedSizeFailures, 
            totalShallowSize, formatSize(totalShallowSize),
            totalRetainedSize, formatSize(totalRetainedSize),
            String.format("%.1f", (double)totalRetainedSize / totalShallowSize * 100));
        
        return totalRetainedSize;
    }
    
    /**
     * 计算一组实例的总深度大小（手动计算，备用方法）
     */
    private long calculateTotalDeepSize(List<Instance> instances) {
        long totalDeepSize = 0;
        
        // 为每个实例单独计算深度大小，不使用全局visited避免遗漏
        for (Instance instance : instances) {
            Set<Long> instanceVisited = new HashSet<>(); // 每个实例用独立的visited集合
            totalDeepSize += calculateDeepSize(instance, instanceVisited, 0, 5);
        }
        
        return totalDeepSize;
    }
    
    /**
     * 递归计算单个对象的深度大小
     * @param obj 要计算的对象
     * @param visited 已访问的对象ID集合，防止循环引用
     * @param depth 当前递归深度
     * @param maxDepth 最大递归深度限制
     * @return 深度大小（包含所有引用对象）
     */
    private long calculateDeepSize(Instance obj, Set<Long> visited, int depth, int maxDepth) {
        if (obj == null || depth >= maxDepth) {
            return 0;
        }
        
        long objId = obj.getInstanceId();
        if (visited.contains(objId)) {
            return 0; // 已经计算过，避免重复
        }
        
        visited.add(objId);
        long totalSize = obj.getSize(); // 从浅表大小开始
        
        try {
            // 遍历所有对象字段
            for (FieldValue fieldValue : obj.getFieldValues()) {
                if (fieldValue instanceof ObjectFieldValue) {
                    ObjectFieldValue objFieldValue = (ObjectFieldValue) fieldValue;
                    Instance referencedObj = objFieldValue.getInstance();
                    
                    if (referencedObj != null) {
                        long refSize = calculateDeepSize(referencedObj, visited, depth + 1, maxDepth);
                        totalSize += refSize;
                        
                        // 记录大对象引用
                        if (refSize > 100 * 1024) { // 大于100KB的引用
                            log.debug("发现大对象引用: {} -> {}, 大小: {}", 
                                obj.getJavaClass().getName(), 
                                referencedObj.getJavaClass().getName(), 
                                formatSize(refSize));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 如果访问字段失败，记录日志但继续
            log.debug("计算深度大小时访问对象字段失败: {}, 对象类型: {}", 
                e.getMessage(), obj.getJavaClass().getName());
        }
        
        return totalSize;
    }
    
    /**
     * 判断是否为JVM生成的类（Lambda、CGLIB代理等）
     */
    private static boolean isGeneratedClass(String className) {
        return className.contains("$$Lambda") || 
               className.contains("$Lambda") ||
               className.contains("$$EnhancerBySpringCGLIB$$") ||
               className.contains("$$FastClassBySpringCGLIB$$") ||
               className.contains("$Proxy$") ||
               className.contains("$$SpringCGLIB$$");
    }
    
    /**
     * 判断是否为集合类型
     */
    private boolean isCollectionType(String className) {
        return className.contains("ArrayList") || 
               className.contains("LinkedList") ||
               className.contains("HashMap") ||
               className.contains("LinkedHashMap") ||
               className.contains("TreeMap") ||
               className.contains("HashSet") ||
               className.contains("LinkedHashSet") ||
               className.contains("TreeSet") ||
               className.contains("Vector") ||
               className.contains("Stack") ||
               className.contains("ConcurrentHashMap");
    }
}