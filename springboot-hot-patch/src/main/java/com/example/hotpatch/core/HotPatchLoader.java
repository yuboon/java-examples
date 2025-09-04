package com.example.hotpatch.core;

import com.example.hotpatch.annotation.HotPatch;
import com.example.hotpatch.annotation.PatchType;
import com.example.hotpatch.config.HotPatchProperties;
import com.example.hotpatch.instrumentation.InstrumentationHolder;
import com.example.hotpatch.model.PatchInfo;
import com.example.hotpatch.model.PatchResult;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.*;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 增强版补丁加载器核心类
 */
@Component
@Slf4j
public class HotPatchLoader {
    
    private final ConfigurableApplicationContext applicationContext;
    private final HotPatchProperties properties;
    private final Map<String, PatchInfo> loadedPatches = new ConcurrentHashMap<>();
    private final Map<String, Object> originalBeans = new ConcurrentHashMap<>();  // 保存原始Bean用于回滚
    private final Map<String, BeanDefinition> originalBeanDefinitions = new ConcurrentHashMap<>();  // 保存原始Bean定义
    private final Map<String, Class<?>> originalBeanTypes = new ConcurrentHashMap<>();  // 保存原始Bean类型
    private final Map<String, byte[]> originalClassBytecode = new ConcurrentHashMap<>();  // 保存原始类字节码
    private final Map<String, byte[]> originalMethodBytecode = new ConcurrentHashMap<>();  // 保存原始方法字节码
    private final Map<String, PatchClassLoader> patchClassLoaders = new ConcurrentHashMap<>();  // 保存补丁类加载器用于真正卸载
    private final Instrumentation instrumentation;
    
    public HotPatchLoader(ConfigurableApplicationContext applicationContext, 
                         HotPatchProperties properties) {
        this.applicationContext = applicationContext;
        this.properties = properties;
        // 获取 Instrumentation 实例
        this.instrumentation = InstrumentationHolder.getInstrumentation();
    }
    
    /**
     * 加载热补丁 - 支持任意类替换
     * @param patchName 补丁名称
     * @param version 版本号
     */
    public PatchResult loadPatch(String patchName, String version) {
        if (!properties.isEnabled()) {
            return PatchResult.failed("热补丁功能未启用");
        }
        
        // 检查补丁是否已经加载
        if (loadedPatches.containsKey(patchName)) {
            PatchInfo existingPatch = loadedPatches.get(patchName);
            String existingVersion = existingPatch.getVersion();
            
            if (version.equals(existingVersion)) {
                log.warn("补丁 {}:{} 已经加载，跳过重复操作", patchName, version);
                return PatchResult.failed("补丁 " + patchName + ":" + version + " 已经加载，请先卸载后再重新加载");
            } else {
                log.warn("补丁 {} 已加载版本 {}，尝试加载新版本 {}，将先自动卸载旧版本", 
                    patchName, existingVersion, version);
                
                // 自动卸载旧版本
                PatchResult rollbackResult = rollbackPatch(patchName);
                if (!rollbackResult.isSuccess()) {
                    return PatchResult.failed("无法卸载已存在的补丁版本 " + existingVersion + ": " + rollbackResult.getMessage());
                }
                
                log.info("已成功卸载旧版本 {}:{}，继续加载新版本", patchName, existingVersion);
            }
        }
        
        try {
            // 1. 验证补丁文件
            File patchFile = validatePatchFile(patchName, version);
            
            // 2. 创建专用的类加载器
            PatchClassLoader patchClassLoader = createPatchClassLoader(patchFile);
            
            // 3. 加载补丁类
            Class<?> patchClass = loadPatchClass(patchClassLoader, patchName);
            
            // 4. 保存类加载器用于后续卸载
            patchClassLoaders.put(patchName, patchClassLoader);
            
            // 5. 获取补丁注解信息
            HotPatch patchAnnotation = patchClass.getAnnotation(HotPatch.class);
            if (patchAnnotation == null) {
                return PatchResult.failed("补丁类缺少 @HotPatch 注解");
            }
            
            // 6. 根据补丁类型选择替换策略
            PatchType patchType = patchAnnotation.type();
            switch (patchType) {
                case SPRING_BEAN:
                    replaceSpringBean(patchClass, patchAnnotation);
                    break;
                case JAVA_CLASS:
                    replaceJavaClass(patchClass, patchAnnotation);
                    break;
                case STATIC_METHOD:
                    replaceStaticMethod(patchClass, patchAnnotation);
                    break;
                case INSTANCE_METHOD:
                    return PatchResult.failed("实例方法替换暂未实现，请使用动态代理方式");
                default:
                    return PatchResult.failed("不支持的补丁类型: " + patchType);
            }
            
            // 7. 记录补丁信息
            PatchInfo patchInfo = new PatchInfo(patchName, version, 
                patchClass, patchType, System.currentTimeMillis());
            loadedPatches.put(patchName, patchInfo);
            
            // 8. 验证补丁加载是否成功（特别是Spring Bean类型）
            if (patchType == PatchType.SPRING_BEAN) {
                verifyPatchLoading(patchInfo);
            }
            
            log.info("热补丁 {}:{} ({}) 加载成功", patchName, version, patchType);
            return PatchResult.success("补丁加载成功");
            
        } catch (Exception e) {
            log.error("热补丁加载失败: {}", e.getMessage(), e);
            return PatchResult.failed("补丁加载失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取已加载的补丁列表
     */
    public List<PatchInfo> getLoadedPatches() {
        return loadedPatches.values().stream().toList();
    }
    
    /**
     * 回滚补丁 - 真正的回滚实现
     */
    public PatchResult rollbackPatch(String patchName) {
        PatchInfo patchInfo = loadedPatches.get(patchName);
        if (patchInfo == null) {
            return PatchResult.failed("补丁不存在: " + patchName);
        }
        
        try {
            // 根据补丁类型执行相应的回滚操作
            switch (patchInfo.getPatchType()) {
                case SPRING_BEAN:
                    rollbackSpringBean(patchInfo);
                    break;
                case JAVA_CLASS:
                    rollbackJavaClass(patchInfo);
                    break;
                case STATIC_METHOD:
                    rollbackStaticMethod(patchInfo);
                    break;
                default:
                    return PatchResult.failed("不支持的补丁类型回滚: " + patchInfo.getPatchType());
            }
            
            // 从已加载补丁列表中移除
            loadedPatches.remove(patchName);
            
            // 清理补丁类加载器（确保类真正卸载）
            PatchClassLoader patchClassLoader = patchClassLoaders.remove(patchName);
            if (patchClassLoader != null) {
                try {
                    patchClassLoader.clearPatchClasses();
                    patchClassLoader.close();
                    log.info("✅ 已清理补丁类加载器: {}", patchName);
                } catch (Exception e) {
                    log.warn("清理补丁类加载器时出现异常: {}", e.getMessage());
                }
            }
            
            // 强制垃圾回收以清理类元数据
            System.gc();
            log.info("已触发垃圾回收以清理类缓存");
            
            log.info("✅ 补丁 {} ({}) 回滚成功", patchName, patchInfo.getPatchType());
            return PatchResult.success("补丁回滚成功");
            
        } catch (Exception e) {
            log.error("补丁回滚失败: {}", e.getMessage(), e);
            return PatchResult.failed("补丁回滚失败: " + e.getMessage());
        }
    }
    
    private File validatePatchFile(String patchName, String version) throws IOException {
        // 构建补丁文件路径
        String fileName = String.format("%s-%s.jar", patchName, version);
        File patchFile = Paths.get(properties.getPath(), fileName).toFile();
        
        if (!patchFile.exists()) {
            throw new FileNotFoundException("补丁文件不存在: " + fileName);
        }
        
        // 验证文件完整性
        if (!isValidPatchFile(patchFile)) {
            throw new SecurityException("补丁文件验证失败");
        }
        
        return patchFile;
    }
    
    /**
     * 创建专用的类加载器 - 增强版，支持真正的类隔离和卸载
     */
    private PatchClassLoader createPatchClassLoader(File patchFile) throws MalformedURLException {
        URL[] urls = {patchFile.toURI().toURL()};
        // 创建独立的类加载器，避免与父类加载器共享类缓存
        return new PatchClassLoader(urls, Thread.currentThread().getContextClassLoader());
    }
    
    private Class<?> loadPatchClass(PatchClassLoader classLoader, String patchName) 
            throws ClassNotFoundException {
        // 尝试多种方式加载补丁类
        String[] possibleClassNames = {
            // 1. 完整包名格式
            "com.example.hotpatch.patches." + patchName + "Patch",
            // 2. 简单类名格式
            patchName + "Patch",
            // 3. 如果patchName本身就包含包名
            patchName
        };
        
        ClassNotFoundException lastException = null;
        for (String className : possibleClassNames) {
            try {
                return classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                lastException = e;
                log.debug("尝试加载类 {} 失败: {}", className, e.getMessage());
            }
        }
        
        throw new ClassNotFoundException("无法加载补丁类，尝试的类名: " + 
            String.join(", ", possibleClassNames), lastException);
    }
    
    /**
     * 替换Spring Bean - 增强版，支持真正的回滚，解决类型注入问题
     */
    private void replaceSpringBean(Class<?> patchClass, HotPatch annotation) {
        ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
        DefaultListableBeanFactory defaultBeanFactory = (DefaultListableBeanFactory) beanFactory;
        String originalBeanName = annotation.originalBean();
        
        if (!StringUtils.hasText(originalBeanName)) {
            throw new IllegalArgumentException("Spring Bean补丁必须指定originalBean名称");
        }
        
        try {
            // 1. 获取原始Bean的类型信息
            Class<?> originalBeanType = null;
            if (beanFactory.containsBean(originalBeanName)) {
                Object originalBeanInstance = beanFactory.getBean(originalBeanName);
                originalBeanType = originalBeanInstance.getClass();
                // 保存原始Bean类型用于回滚
                originalBeanTypes.put(originalBeanName, originalBeanType);
                log.info("检测到原始Bean类型: {} -> {}", originalBeanName, originalBeanType.getName());
            }
            
            // 2. 检查补丁类是否已经被Spring自动注册（防止重复Bean问题）
            String patchBeanName = getPatchBeanName(patchClass);
            if (beanFactory.containsBeanDefinition(patchBeanName)) {
                log.info("发现补丁类已被自动注册为Bean: {}，将其移除以避免冲突", patchBeanName);
                defaultBeanFactory.removeBeanDefinition(patchBeanName);
                if (beanFactory.containsSingleton(patchBeanName)) {
                    defaultBeanFactory.destroySingleton(patchBeanName);
                }
            }
            
            // 3. 备份原始Bean和Bean定义（用于回滚）
            if (beanFactory.containsBeanDefinition(originalBeanName)) {
                // 保存原始Bean定义
                BeanDefinition originalBeanDef = beanFactory.getBeanDefinition(originalBeanName);
                originalBeanDefinitions.put(originalBeanName, originalBeanDef);
                
                // 保存原始Bean实例（如果是单例）
                if (beanFactory.isSingleton(originalBeanName)) {
                    Object originalBean = beanFactory.getBean(originalBeanName);
                    originalBeans.put(originalBeanName, originalBean);
                }
                
                // 4. 销毁原始Bean实例
                defaultBeanFactory.destroySingleton(originalBeanName);
                
                // 5. 移除原始Bean定义
                defaultBeanFactory.removeBeanDefinition(originalBeanName);
            }
            
            // 6. 创建补丁Bean定义，确保正确的自动装配
            GenericBeanDefinition patchBeanDefinition = new GenericBeanDefinition();
            patchBeanDefinition.setBeanClass(patchClass);
            
            // 保持原始Bean的作用域和其他属性
            if (originalBeanDefinitions.containsKey(originalBeanName)) {
                BeanDefinition originalDef = originalBeanDefinitions.get(originalBeanName);
                patchBeanDefinition.setScope(originalDef.getScope());
                patchBeanDefinition.setLazyInit(originalDef.isLazyInit());
                patchBeanDefinition.setPrimary(originalDef.isPrimary());
                
                // 复制依赖注入信息
                if (originalDef.getPropertyValues() != null) {
                    patchBeanDefinition.setPropertyValues(originalDef.getPropertyValues());
                }
                
                // 保持原有的自动装配模式
                if (originalDef instanceof AbstractBeanDefinition) {
                    AbstractBeanDefinition origAbsDef = (AbstractBeanDefinition) originalDef;
                    patchBeanDefinition.setAutowireMode(origAbsDef.getAutowireMode());
                    patchBeanDefinition.setDependencyCheck(origAbsDef.getDependencyCheck());
                }
            } else {
                // 如果没有原始定义，使用默认设置
                patchBeanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
                patchBeanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
            }
            
            // 设置为主要Bean，避免类型冲突
            patchBeanDefinition.setPrimary(true);
            
            // 7. 注册补丁Bean定义 - 使用原始Bean名称
            defaultBeanFactory.registerBeanDefinition(originalBeanName, patchBeanDefinition);
            
            // 8. 重要：如果有原始Bean类型，还需要按类型注册别名
            if (originalBeanType != null && !originalBeanType.equals(patchClass)) {
                // 为原始类型创建一个别名，确保按类型注入可以工作
                String typeBasedBeanName = originalBeanType.getSimpleName().substring(0, 1).toLowerCase() + 
                                         originalBeanType.getSimpleName().substring(1);
                
                if (!typeBasedBeanName.equals(originalBeanName)) {
                    defaultBeanFactory.registerAlias(originalBeanName, typeBasedBeanName);
                    log.info("为原始类型注册别名: {} -> {}", originalBeanName, typeBasedBeanName);
                }
            }
            
            // 9. 手动处理Spring注解（如果补丁类有Spring注解）
            processSpringAnnotations(patchClass, originalBeanName, defaultBeanFactory);
            
            // 10. 预实例化单例Bean（确保依赖注入正确）
            if (patchBeanDefinition.isSingleton()) {
                Object patchBean = beanFactory.getBean(originalBeanName);
                log.info("✅ 补丁Bean实例化成功: {} -> {} (类型: {})", 
                    originalBeanName, patchBean.getClass().getName(), patchBean.getClass().getInterfaces());
                    
                // 验证Bean是否可以通过类型获取
                if (originalBeanType != null) {
                    try {
                        Object beanByType = beanFactory.getBean(originalBeanType);
                        log.info("✅ 按类型获取Bean成功: {} -> {}", originalBeanType.getName(), beanByType.getClass().getName());
                        
                        // 强制更新所有Bean中已注入的字段引用到新的补丁Bean
                        updateInjectedFieldReferences(beanFactory, originalBeanType, patchBean);
                        
                    } catch (Exception e) {
                        log.warn("⚠️ 按类型获取Bean失败: {}", e.getMessage());
                        
                        // 如果按类型获取失败，尝试手动注册类型映射
                        String[] beanNames = defaultBeanFactory.getBeanNamesForType(originalBeanType);
                        if (beanNames.length == 0) {
                            // 手动注册类型映射
                            defaultBeanFactory.registerResolvableDependency(originalBeanType, patchBean);
                            log.info("✅ 手动注册类型依赖映射: {} -> {}", originalBeanType.getName(), patchBean.getClass().getName());
                            
                            // 再次尝试更新字段引用
                            updateInjectedFieldReferences(beanFactory, originalBeanType, patchBean);
                        }
                    }
                }
            }
            
            log.info("✅ 已成功替换Spring Bean: {} -> {}", originalBeanName, patchClass.getName());
            
        } catch (Exception e) {
            // 如果替换失败，尝试恢复原始状态
            try {
                restoreBeanDefinition(originalBeanName);
            } catch (Exception restoreEx) {
                log.error("恢复Bean定义失败", restoreEx);
            }
            throw new RuntimeException("替换Spring Bean失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 替换普通Java类 - 使用方法委托的简化方式，支持回滚
     */
    private void replaceJavaClass(Class<?> patchClass, HotPatch annotation) {
        try {
            String originalClassName = annotation.originalClass();
            if (!StringUtils.hasText(originalClassName)) {
                // 如果没有指定原始类名，则根据补丁类名推断
                originalClassName = patchClass.getName().replace("Patch", "");
            }
            
            // 获取原始类
            Class<?> originalClass = Class.forName(originalClassName);
            
            // 保存原始类的字节码用于回滚
            if (!originalClassBytecode.containsKey(originalClassName)) {
                byte[] originalBytes = getClassBytes(originalClass);
                originalClassBytecode.put(originalClassName, originalBytes);
                log.info("已保存原始类字节码用于回滚: {} ({} bytes)", originalClassName, originalBytes.length);
            }
            
            // 使用简化的方法委托替换
            byte[] modifiedClassBytes = createDelegatingClass(originalClass, patchClass);
            
            // 创建类定义用于重定义
            ClassDefinition classDefinition = new ClassDefinition(originalClass, modifiedClassBytes);
            
            // 使用Instrumentation重定义类
            if (instrumentation != null && instrumentation.isRedefineClassesSupported()) {
                instrumentation.redefineClasses(classDefinition);
                log.info("✅ 已替换Java类: {} -> {} ({} bytes)", originalClassName, patchClass.getName(), modifiedClassBytes.length);
            } else {
                throw new UnsupportedOperationException("当前JVM不支持类重定义");
            }
            
        } catch (Exception e) {
            throw new RuntimeException("替换Java类失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 替换静态方法 - 通过ASM字节码操作，支持回滚
     */
    private void replaceStaticMethod(Class<?> patchClass, HotPatch annotation) {
        try {
            String originalClassName = annotation.originalClass();
            String methodName = annotation.methodName();
            
            if (!StringUtils.hasText(originalClassName) || !StringUtils.hasText(methodName)) {
                throw new IllegalArgumentException("静态方法替换需要指定原始类名和方法名");
            }
            
            // 获取原始类
            Class<?> originalClass = Class.forName(originalClassName);
            
            // 保存原始类的字节码用于回滚
            String methodKey = originalClassName + "." + methodName;
            if (!originalMethodBytecode.containsKey(methodKey)) {
                byte[] originalBytes = getClassBytes(originalClass);
                originalMethodBytecode.put(methodKey, originalBytes);
                log.info("已保存原始方法字节码用于回滚: {} ({} bytes)", methodKey, originalBytes.length);
            }
            
            // 使用ASM修改字节码，将原方法的调用重定向到补丁方法
            byte[] modifiedBytes = modifyClassBytecode(originalClass, methodName, patchClass);
            
            // 重定义类
            ClassDefinition classDefinition = new ClassDefinition(originalClass, modifiedBytes);
            if (instrumentation != null && instrumentation.isRedefineClassesSupported()) {
                instrumentation.redefineClasses(classDefinition);
                log.info("✅ 已替换静态方法: {}.{} -> {}", originalClassName, methodName, patchClass.getName());
            } else {
                throw new UnsupportedOperationException("当前JVM不支持类重定义");
            }
            
        } catch (Exception e) {
            throw new RuntimeException("替换静态方法失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取类的字节码
     */
    private byte[] getClassBytes(Class<?> clazz) throws IOException {
        String className = clazz.getName();
        String classFilePath = className.replace('.', '/') + ".class";
        
        try (InputStream is = clazz.getClassLoader().getResourceAsStream(classFilePath);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            if (is == null) {
                throw new IOException("无法找到类文件: " + classFilePath);
            }
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }
    
    /**
     * 使用ASM修改类字节码
     */
    private byte[] modifyClassBytecode(Class<?> originalClass, String methodName, Class<?> patchClass) {
        try {
            ClassReader classReader = new ClassReader(getClassBytes(originalClass));
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            
            ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM9, classWriter) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, 
                        String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    
                    if (methodName.equals(name)) {
                        // 返回一个修改方法体的MethodVisitor
                        return new MethodVisitor(Opcodes.ASM9, mv) {
                            @Override
                            public void visitCode() {
                                super.visitCode();
                                // 清空原方法体，调用补丁方法
                                redirectToPatchMethod(mv, patchClass, methodName, descriptor);
                            }
                        };
                    }
                    return mv;
                }
            };
            
            classReader.accept(classVisitor, 0);
            return classWriter.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("修改字节码失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建委托类 - 将原始类的方法重定向到补丁类
     */
    private byte[] createDelegatingClass(Class<?> originalClass, Class<?> patchClass) {
        try {
            byte[] originalBytes = getClassBytes(originalClass);
            ClassReader classReader = new ClassReader(originalBytes);
            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
            
            // 获取补丁类的方法列表
            Set<String> patchMethods = getPatchMethods(patchClass);
            log.info("补丁类 {} 中的方法: {}", patchClass.getSimpleName(), patchMethods);
            
            if (patchMethods.isEmpty()) {
                log.warn("补丁类中没有找到任何方法，返回原始字节码");
                return originalBytes;
            }
            
            ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM9, classWriter) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, 
                        String signature, String[] exceptions) {
                    
                    String methodKey = name + descriptor;
                    
                    // 处理补丁类中存在的所有方法（静态方法和实例方法）
                    if (patchMethods.contains(methodKey)) {
                        boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
                        log.info("替换方法: {} (静态: {})", methodKey, isStatic);
                        
                        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                        if (isStatic) {
                            return new StaticMethodReplacer(mv, patchClass, name, descriptor);
                        } else {
                            return new InstanceMethodReplacer(mv, patchClass, name, descriptor);
                        }
                    }
                    
                    // 保留其他方法不变
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
            };
            
            classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
            byte[] result = classWriter.toByteArray();
            
            log.info("成功生成委托类字节码: {} -> {} bytes", originalClass.getSimpleName(), result.length);
            return result;
            
        } catch (Exception e) {
            log.error("创建委托类失败", e);
            throw new RuntimeException("创建委托类失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取补丁类的方法列表
     */
    private Set<String> getPatchMethods(Class<?> patchClass) {
        Set<String> methods = new HashSet<>();
        
        java.lang.reflect.Method[] declaredMethods = patchClass.getDeclaredMethods();
        for (java.lang.reflect.Method method : declaredMethods) {
            // 跳过构造函数和特殊方法
            if (!method.getName().equals("<init>") && !method.getName().equals("<clinit>")) {
                String descriptor = getMethodDescriptor(method);
                methods.add(method.getName() + descriptor);
            }
        }
        
        return methods;
    }
    
    /**
     * 获取方法描述符
     */
    private String getMethodDescriptor(java.lang.reflect.Method method) {
        StringBuilder sb = new StringBuilder("(");
        
        // 参数类型
        for (Class<?> paramType : method.getParameterTypes()) {
            sb.append(getTypeDescriptor(paramType));
        }
        
        sb.append(")");
        
        // 返回类型
        sb.append(getTypeDescriptor(method.getReturnType()));
        
        return sb.toString();
    }
    
    /**
     * 获取类型描述符
     */
    private String getTypeDescriptor(Class<?> type) {
        if (type.isPrimitive()) {
            if (type == boolean.class) return "Z";
            if (type == byte.class) return "B";
            if (type == char.class) return "C";
            if (type == short.class) return "S";
            if (type == int.class) return "I";
            if (type == long.class) return "J";
            if (type == float.class) return "F";
            if (type == double.class) return "D";
            if (type == void.class) return "V";
        } else if (type.isArray()) {
            return "[" + getTypeDescriptor(type.getComponentType());
        } else {
            return "L" + type.getName().replace('.', '/') + ";";
        }
        return "";
    }
    
    /**
     * 实例方法替换器 - 支持实例方法替换
     */
    private static class InstanceMethodReplacer extends MethodVisitor {
        private final Class<?> patchClass;
        private final String methodName;
        private final String descriptor;
        private boolean codeVisited = false;
        
        public InstanceMethodReplacer(MethodVisitor mv, Class<?> patchClass, 
                                    String methodName, String descriptor) {
            super(Opcodes.ASM9, mv);
            this.patchClass = patchClass;
            this.methodName = methodName;
            this.descriptor = descriptor;
        }
        
        @Override
        public void visitCode() {
            if (!codeVisited) {
                super.visitCode();
                generateDelegateCall();
                codeVisited = true;
            }
        }
        
        private void generateDelegateCall() {
            try {
                // 解析方法描述符
                Type methodType = Type.getMethodType(descriptor);
                Type[] argumentTypes = methodType.getArgumentTypes();
                Type returnType = methodType.getReturnType();
                
                String patchClassName = patchClass.getName().replace('.', '/');
                
                // 创建补丁类实例
                super.visitTypeInsn(Opcodes.NEW, patchClassName);
                super.visitInsn(Opcodes.DUP);
                super.visitMethodInsn(Opcodes.INVOKESPECIAL, patchClassName, "<init>", "()V", false);
                
                // 加载所有参数（跳过this参数，从索引1开始）
                int localIndex = 1; // 跳过this
                for (Type argType : argumentTypes) {
                    super.visitVarInsn(argType.getOpcode(Opcodes.ILOAD), localIndex);
                    localIndex += argType.getSize();
                }
                
                // 调用补丁类的实例方法
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
                    patchClassName,
                    methodName, 
                    descriptor, 
                    false);
                
                // 返回结果
                super.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
                
                // 计算栈和局部变量大小
                int maxStack = Math.max(argumentTypes.length + 3, 3); // +3 for new, dup, instance
                int maxLocals = localIndex;
                super.visitMaxs(maxStack, maxLocals);
                super.visitEnd();
                
            } catch (Exception e) {
                throw new RuntimeException("生成实例方法委托调用失败: " + e.getMessage(), e);
            }
        }
        
        // 忽略原始方法的所有其他指令
        @Override
        public void visitInsn(int opcode) {
            // 忽略
        }
        
        @Override
        public void visitVarInsn(int opcode, int var) {
            // 忽略
        }
        
        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            // 忽略
        }
        
        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            // 忽略
        }
        
        @Override
        public void visitTypeInsn(int opcode, String type) {
            // 忽略
        }
        
        @Override
        public void visitJumpInsn(int opcode, Label label) {
            // 忽略
        }
        
        @Override
        public void visitLdcInsn(Object value) {
            // 忽略
        }
        
        @Override
        public void visitIincInsn(int var, int increment) {
            // 忽略
        }
        
        @Override
        public void visitLabel(Label label) {
            // 忽略
        }
        
        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            // 已在generateDelegateCall中处理
        }
        
        @Override
        public void visitEnd() {
            // 已在generateDelegateCall中处理
        }
    }
    
    /**
     * 静态方法替换器 - 更安全的实现
     */
    private static class StaticMethodReplacer extends MethodVisitor {
        private final Class<?> patchClass;
        private final String methodName;
        private final String descriptor;
        private boolean codeVisited = false;
        
        public StaticMethodReplacer(MethodVisitor mv, Class<?> patchClass, 
                                  String methodName, String descriptor) {
            super(Opcodes.ASM9, mv);
            this.patchClass = patchClass;
            this.methodName = methodName;
            this.descriptor = descriptor;
        }
        
        @Override
        public void visitCode() {
            if (!codeVisited) {
                super.visitCode();
                generateDelegateCall();
                codeVisited = true;
            }
        }
        
        private void generateDelegateCall() {
            try {
                // 解析方法描述符
                Type methodType = Type.getMethodType(descriptor);
                Type[] argumentTypes = methodType.getArgumentTypes();
                Type returnType = methodType.getReturnType();
                
                // 加载所有参数到栈上
                int localIndex = 0;
                for (Type argType : argumentTypes) {
                    super.visitVarInsn(argType.getOpcode(Opcodes.ILOAD), localIndex);
                    localIndex += argType.getSize();
                }
                
                // 调用补丁类的静态方法
                String patchClassName = patchClass.getName().replace('.', '/');
                super.visitMethodInsn(Opcodes.INVOKESTATIC, 
                    patchClassName,
                    methodName, 
                    descriptor, 
                    false);
                
                // 返回结果
                super.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
                
                // 计算栈和局部变量大小
                int maxStack = Math.max(argumentTypes.length, 1);
                int maxLocals = localIndex;
                super.visitMaxs(maxStack, maxLocals);
                super.visitEnd();
                
            } catch (Exception e) {
                throw new RuntimeException("生成委托调用失败: " + e.getMessage(), e);
            }
        }
        
        // 忽略原始方法的所有其他指令
        @Override
        public void visitInsn(int opcode) {
            // 忽略
        }
        
        @Override
        public void visitVarInsn(int opcode, int var) {
            // 忽略
        }
        
        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            // 忽略
        }
        
        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            // 忽略
        }
        
        @Override
        public void visitTypeInsn(int opcode, String type) {
            // 忽略
        }
        
        @Override
        public void visitJumpInsn(int opcode, Label label) {
            // 忽略
        }
        
        @Override
        public void visitLdcInsn(Object value) {
            // 忽略
        }
        
        @Override
        public void visitIincInsn(int var, int increment) {
            // 忽略
        }
        
        @Override
        public void visitLabel(Label label) {
            // 忽略
        }
        
        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            // 已在generateDelegateCall中处理
        }
        
        @Override
        public void visitEnd() {
            // 已在generateDelegateCall中处理
        }
    }
    
    private void redirectToPatchMethod(MethodVisitor mv, Class<?> patchClass, 
            String methodName, String descriptor) {
        // 生成调用补丁方法的字节码（简化版本）
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
            patchClass.getName().replace('.', '/'),
            methodName, descriptor, false);
        mv.visitInsn(Opcodes.ARETURN); // 或其他适当的返回指令
    }
    
    private boolean isValidPatchFile(File file) {
        try {
            // 1. 文件大小检查
            if (file.length() > properties.getMaxFileSize()) {
                return false;
            }
            
            // 2. 文件类型检查
            if (!file.getName().endsWith(".jar")) {
                return false;
            }
            
            // 3. 简单的完整性检查
            return file.length() > 0;
            
        } catch (Exception e) {
            log.error("补丁文件验证失败", e);
            return false;
        }
    }
    
    /**
     * 恢复Bean定义的辅助方法
     */
    private void restoreBeanDefinition(String beanName) {
        if (originalBeanDefinitions.containsKey(beanName)) {
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
            
            // 移除当前的Bean定义
            if (beanFactory.containsBeanDefinition(beanName)) {
                beanFactory.destroySingleton(beanName);
                beanFactory.removeBeanDefinition(beanName);
            }
            
            // 恢复原始Bean定义
            BeanDefinition originalDef = originalBeanDefinitions.get(beanName);
            beanFactory.registerBeanDefinition(beanName, originalDef);
            
            log.info("已恢复Bean定义: {}", beanName);
        }
    }
    
    /**
     * 处理Spring注解 - 确保补丁Bean能被Spring正确管理
     */
    private void processSpringAnnotations(Class<?> patchClass, String beanName, DefaultListableBeanFactory beanFactory) {
        // 检查是否有@Service, @Component, @Repository, @Controller等注解
        if (patchClass.isAnnotationPresent(org.springframework.stereotype.Service.class)) {
            log.debug("补丁类 {} 包含 @Service 注解", patchClass.getName());
            // Spring会自动处理这些注解，我们主要确保Bean定义正确
        }
        if (patchClass.isAnnotationPresent(Component.class)) {
            log.debug("补丁类 {} 包含 @Component 注解", patchClass.getName());
        }
        
        // 确保Bean定义启用了注解驱动的依赖注入
        BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
        if (beanDefinition instanceof GenericBeanDefinition) {
            GenericBeanDefinition genDef = (GenericBeanDefinition) beanDefinition;
            // 确保启用了注解注入
            genDef.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        }
    }
    
    /**
     * Spring Bean回滚实现 - 完整的回滚逻辑，强制刷新所有依赖，确保没有重复Bean
     */
    private void rollbackSpringBean(PatchInfo patchInfo) {
        HotPatch annotation = patchInfo.getPatchClass().getAnnotation(HotPatch.class);
        String beanName = annotation.originalBean();
        
        if (!StringUtils.hasText(beanName)) {
            throw new IllegalArgumentException("无法回滚：缺少原始Bean名称");
        }
        
        try {
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
            Class<?> originalBeanType = originalBeanTypes.get(beanName);
            
            log.info("开始回滚Spring Bean: {} (类型: {})", beanName, 
                originalBeanType != null ? originalBeanType.getName() : "未知");
            
            // === 第一阶段：彻底清理补丁类和相关缓存 ===
            
            // 1. 强制清理补丁类的类加载器
            PatchClassLoader patchClassLoader = patchClassLoaders.get(patchInfo.getName());
            if (patchClassLoader != null) {
                patchClassLoader.clearPatchClasses();
                log.info("已清理补丁类加载器缓存");
            }
            
            // 2. 查找并清理所有可能的补丁Bean（包括自动注册的）
            String patchBeanName = getPatchBeanName(patchInfo.getPatchClass());
            Set<String> allPatchBeanNames = new HashSet<>();
            allPatchBeanNames.add(patchBeanName);
            
            // 查找所有可能的补丁Bean名称变体
            String[] allBeanNames = beanFactory.getBeanDefinitionNames();
            for (String candidateName : allBeanNames) {
                if (candidateName.contains("Patch") || candidateName.toLowerCase().contains("patch")) {
                    try {
                        Class<?> candidateType = beanFactory.getType(candidateName);
                        if (candidateType != null && candidateType.equals(patchInfo.getPatchClass())) {
                            allPatchBeanNames.add(candidateName);
                        }
                    } catch (Exception e) {
                        log.debug("检查候选Bean类型时出现异常: {} - {}", candidateName, e.getMessage());
                    }
                }
            }
            
            log.info("发现需要清理的补丁Bean: {}", allPatchBeanNames);
            
            // 3. 彻底清理所有补丁Bean（包括单例缓存、Bean定义、类型映射）
            for (String patchName : allPatchBeanNames) {
                if (beanFactory.containsSingleton(patchName)) {
                    beanFactory.destroySingleton(patchName);
                    log.info("已销毁补丁Bean实例: {}", patchName);
                }
                if (beanFactory.containsBeanDefinition(patchName)) {
                    beanFactory.removeBeanDefinition(patchName);
                    log.info("已移除补丁Bean定义: {}", patchName);
                }
            }
            
            // === 第二阶段：清理原始Bean和依赖关系 ===
            
            // 4. 收集所有依赖该Bean的其他Bean（这些Bean需要重新实例化）
            Set<String> dependentBeans = findDependentBeans(beanFactory, beanName, originalBeanType);
            log.info("发现依赖Bean: {}", dependentBeans);
            
            // 5. 销毁所有依赖的Bean实例（强制重新注入）
            for (String dependentBeanName : dependentBeans) {
                if (beanFactory.containsSingleton(dependentBeanName)) {
                    beanFactory.destroySingleton(dependentBeanName);
                    log.info("已销毁依赖Bean实例: {}", dependentBeanName);
                }
            }
            
            // 6. 清理当前补丁Bean的类型依赖映射
            if (originalBeanType != null) {
                try {
                    // 清理类型依赖映射
                    clearResolvableDependency(beanFactory, originalBeanType);
                    log.info("已清理类型依赖映射: {}", originalBeanType.getName());
                } catch (Exception e) {
                    log.debug("清理类型依赖映射时出现异常（可忽略）: {}", e.getMessage());
                }
            }
            
            // 7. 销毁当前的目标Bean实例（如果还存在）
            if (beanFactory.containsSingleton(beanName)) {
                beanFactory.destroySingleton(beanName);
                log.info("已销毁目标Bean实例: {}", beanName);
            }
            
            // 8. 移除当前Bean定义（如果还存在）
            if (beanFactory.containsBeanDefinition(beanName)) {
                beanFactory.removeBeanDefinition(beanName);
                log.info("已移除目标Bean定义: {}", beanName);
            }
            
            // === 第三阶段：强制清理所有Spring内部缓存 ===
            
            // 9. 清理Spring的内部缓存
            clearSpringInternalCaches(beanFactory, beanName, originalBeanType);
            
            // === 第四阶段：恢复原始Bean定义 ===
            
            // 10. 恢复原始Bean定义
            if (originalBeanDefinitions.containsKey(beanName)) {
                BeanDefinition originalDef = originalBeanDefinitions.get(beanName);
                
                // 创建一个全新的Bean定义实例，避免缓存问题
                GenericBeanDefinition newOriginalDef = new GenericBeanDefinition();
                newOriginalDef.setBeanClass(originalBeanType);
                newOriginalDef.setScope(originalDef.getScope());
                newOriginalDef.setLazyInit(originalDef.isLazyInit());
                newOriginalDef.setPrimary(originalDef.isPrimary());
                newOriginalDef.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
                
                beanFactory.registerBeanDefinition(beanName, newOriginalDef);
                log.info("已恢复原始Bean定义: {}", beanName);
                
                // 11. 强制实例化恢复后的Bean
                if (newOriginalDef.isSingleton()) {
                    Object restoredBean = beanFactory.getBean(beanName);
                    log.info("已强制实例化恢复的Bean: {} -> {}", beanName, restoredBean.getClass().getName());
                    
                    // 12. 重新注册类型依赖映射到恢复的Bean
                    if (originalBeanType != null) {
                        beanFactory.registerResolvableDependency(originalBeanType, restoredBean);
                        log.info("已重新注册类型依赖映射: {} -> {}", originalBeanType.getName(), restoredBean.getClass().getName());
                    }
                }
            } else {
                log.warn("⚠️ 没有找到原始Bean定义，无法完全回滚: {}", beanName);
                return;
            }
            
            // === 第五阶段：强制更新所有已注入的字段引用 ===
            
            // 13. 获取新实例化的Bean
            Object restoredBeanInstance = beanFactory.getBean(beanName);
            
            // 14. 强制更新所有Bean中已注入的字段引用
            updateInjectedFieldReferences(beanFactory, originalBeanType, restoredBeanInstance);
            
            // 15. 强制重新实例化所有依赖的Bean（确保它们使用新的Bean实例）
            for (String dependentBeanName : dependentBeans) {
                try {
                    if (beanFactory.containsBeanDefinition(dependentBeanName)) {
                        Object newDependentBean = beanFactory.getBean(dependentBeanName);
                        log.info("已重新实例化依赖Bean: {} -> {}", dependentBeanName, newDependentBean.getClass().getName());
                        
                        // 验证依赖Bean中的注入是否已经更新
                        verifyDependencyInjection(newDependentBean, beanName, originalBeanType);
                    }
                } catch (Exception e) {
                    log.warn("重新实例化依赖Bean失败: {} - {}", dependentBeanName, e.getMessage());
                }
            }
            
            // === 第六阶段：最终验证 ===
            
            // 14. 最终验证：检查回滚是否真正成功
            if (originalBeanType != null) {
                try {
                    // 检查是否还有多个Bean
                    String[] beanNamesOfType = beanFactory.getBeanNamesForType(originalBeanType);
                    log.info("类型 {} 对应的Bean数量: {} - {}", originalBeanType.getName(), beanNamesOfType.length, 
                        Arrays.toString(beanNamesOfType));
                    
                    if (beanNamesOfType.length > 1) {
                        log.error("❌ 回滚后仍有多个相同类型的Bean: {}", Arrays.toString(beanNamesOfType));
                        throw new RuntimeException("回滚失败：存在多个相同类型的Bean");
                    }
                    
                    Object finalBean = beanFactory.getBean(originalBeanType);
                    log.info("✅ 回滚验证成功 - 按类型获取Bean: {} -> {}", 
                        originalBeanType.getName(), finalBean.getClass().getName());
                        
                    // 检查是否真的是原始类型而不是补丁类型
                    if (finalBean.getClass().getName().contains("Patch")) {
                        log.error("❌ 回滚失败：获取的Bean仍然是补丁类型: {}", finalBean.getClass().getName());
                        throw new RuntimeException("回滚失败：Bean类型未恢复");
                    }
                    
                    // 测试原始Bean的功能是否已恢复
                    // testOriginalBeanFunctionality(finalBean, originalBeanType);
                    
                } catch (Exception e) {
                    log.error("❌ 回滚验证失败: {}", e.getMessage());
                    throw new RuntimeException("回滚验证失败: " + e.getMessage());
                }
            }
            
            // === 第七阶段：清理回滚相关的缓存 ===
            
            // 15. 清理回滚相关的缓存
            originalBeans.remove(beanName);
            originalBeanDefinitions.remove(beanName);
            originalBeanTypes.remove(beanName);
            
            // 16. 强制最后一次垃圾回收
            System.gc();
            Thread.sleep(100); // 给GC一些时间
            log.info("已强制执行垃圾回收");
            
            log.info("✅ Spring Bean {} 已成功回滚到原始状态", beanName);
            
        } catch (Exception e) {
            log.error("Spring Bean回滚过程中出现错误: {}", e.getMessage(), e);
            throw new RuntimeException("Spring Bean回滚失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * Java类回滚实现 - 使用保存的原始字节码恢复类定义
     */
    private void rollbackJavaClass(PatchInfo patchInfo) {
        try {
            HotPatch annotation = patchInfo.getPatchClass().getAnnotation(HotPatch.class);
            String originalClassName = annotation.originalClass();
            
            if (!StringUtils.hasText(originalClassName)) {
                // 如果没有指定原始类名，则根据补丁类名推断
                originalClassName = patchInfo.getPatchClass().getName().replace("Patch", "");
            }
            
            // 检查是否有保存的原始字节码
            byte[] originalBytes = originalClassBytecode.get(originalClassName);
            if (originalBytes == null) {
                throw new IllegalStateException("没有找到原始类的字节码缓存，无法回滚: " + originalClassName);
            }
            
            // 获取原始类
            Class<?> originalClass = Class.forName(originalClassName);
            
            // 使用原始字节码重定义类
            ClassDefinition classDefinition = new ClassDefinition(originalClass, originalBytes);
            
            if (instrumentation != null && instrumentation.isRedefineClassesSupported()) {
                instrumentation.redefineClasses(classDefinition);
                log.info("✅ 已使用原始字节码恢复Java类: {} ({} bytes)", originalClassName, originalBytes.length);
                
                // 清理缓存
                originalClassBytecode.remove(originalClassName);
                
                // 验证类恢复是否成功
                try {
                    // 尝试创建类的实例来验证
                    Object instance = originalClass.getDeclaredConstructor().newInstance();
                    log.info("✅ 类恢复验证成功: {} -> {}", originalClassName, instance.getClass().getName());
                } catch (Exception e) {
                    log.debug("类恢复验证失败（可能正常，如果类没有无参构造器）: {}", e.getMessage());
                }
                
            } else {
                throw new UnsupportedOperationException("当前JVM不支持类重定义，无法回滚");
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Java类回滚失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 静态方法回滚实现 - 使用保存的原始字节码恢复方法定义
     */
    private void rollbackStaticMethod(PatchInfo patchInfo) {
        try {
            HotPatch annotation = patchInfo.getPatchClass().getAnnotation(HotPatch.class);
            String originalClassName = annotation.originalClass();
            String methodName = annotation.methodName();
            
            if (!StringUtils.hasText(originalClassName) || !StringUtils.hasText(methodName)) {
                throw new IllegalArgumentException("静态方法回滚需要指定原始类名和方法名");
            }
            
            // 构建方法键
            String methodKey = originalClassName + "." + methodName;
            
            // 检查是否有保存的原始字节码
            byte[] originalBytes = originalMethodBytecode.get(methodKey);
            if (originalBytes == null) {
                throw new IllegalStateException("没有找到原始方法的字节码缓存，无法回滚: " + methodKey);
            }
            
            // 获取原始类
            Class<?> originalClass = Class.forName(originalClassName);
            
            // 使用原始字节码重定义类（恢复原始方法）
            ClassDefinition classDefinition = new ClassDefinition(originalClass, originalBytes);
            
            if (instrumentation != null && instrumentation.isRedefineClassesSupported()) {
                instrumentation.redefineClasses(classDefinition);
                log.info("✅ 已使用原始字节码恢复静态方法: {} ({} bytes)", methodKey, originalBytes.length);
                
                // 清理缓存
                originalMethodBytecode.remove(methodKey);
                
                // 验证方法恢复是否成功
                try {
                    // 尝试反射获取方法来验证
                    java.lang.reflect.Method[] methods = originalClass.getDeclaredMethods();
                    boolean methodFound = false;
                    for (java.lang.reflect.Method method : methods) {
                        if (method.getName().equals(methodName)) {
                            methodFound = true;
                            log.info("✅ 方法恢复验证成功: {}.{} -> 方法存在且可访问", originalClassName, methodName);
                            break;
                        }
                    }
                    
                    if (!methodFound) {
                        log.warn("⚠️ 方法恢复验证：未找到方法 {}.{}", originalClassName, methodName);
                    }
                    
                } catch (Exception e) {
                    log.debug("方法恢复验证失败: {}", e.getMessage());
                }
                
            } else {
                throw new UnsupportedOperationException("当前JVM不支持类重定义，无法回滚");
            }
            
        } catch (Exception e) {
            throw new RuntimeException("静态方法回滚失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 查找依赖指定Bean的其他Bean
     */
    private Set<String> findDependentBeans(DefaultListableBeanFactory beanFactory, String targetBeanName, Class<?> targetBeanType) {
        Set<String> dependentBeans = new HashSet<>();
        
        try {
            // 获取所有Bean名称
            String[] allBeanNames = beanFactory.getBeanDefinitionNames();
            
            for (String beanName : allBeanNames) {
                if (beanName.equals(targetBeanName)) {
                    continue; // 跳过目标Bean本身
                }
                
                try {
                    BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
                    
                    // 检查是否依赖目标Bean（通过Bean名称）
                    if (beanDef.getDependsOn() != null) {
                        for (String dependsOn : beanDef.getDependsOn()) {
                            if (targetBeanName.equals(dependsOn)) {
                                dependentBeans.add(beanName);
                                break;
                            }
                        }
                    }
                    
                    // 检查是否有该Bean的实例，并通过反射检查是否注入了目标Bean类型
                    if (beanFactory.containsSingleton(beanName) && targetBeanType != null) {
                        Object beanInstance = beanFactory.getSingleton(beanName);
                        if (beanInstance != null && hasFieldOfType(beanInstance, targetBeanType)) {
                            dependentBeans.add(beanName);
                        }
                    }
                    
                } catch (Exception e) {
                    log.debug("检查Bean依赖关系时出现异常: {} - {}", beanName, e.getMessage());
                }
            }
            
            // 特别检查常见的Controller和Service Bean
            String[] commonDependentPatterns = {"Controller", "Service", "Component", "RestController"};
            for (String beanName : allBeanNames) {
                for (String pattern : commonDependentPatterns) {
                    if (beanName.toLowerCase().contains(pattern.toLowerCase()) || 
                        beanFactory.getType(beanName).getSimpleName().contains(pattern)) {
                        dependentBeans.add(beanName);
                        break;
                    }
                }
            }
            
        } catch (Exception e) {
            log.warn("查找依赖Bean时出现异常: {}", e.getMessage());
        }
        
        return dependentBeans;
    }
    
    /**
     * 检查对象是否有指定类型的字段
     */
    private boolean hasFieldOfType(Object obj, Class<?> fieldType) {
        try {
            java.lang.reflect.Field[] fields = obj.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (fieldType.isAssignableFrom(field.getType())) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("检查字段类型时出现异常: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * 验证依赖注入是否正确更新
     */
    private void verifyDependencyInjection(Object dependentBean, String targetBeanName, Class<?> targetBeanType) {
        try {
            if (targetBeanType == null) {
                return;
            }
            
            java.lang.reflect.Field[] fields = dependentBean.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (targetBeanType.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object injectedValue = field.get(dependentBean);
                    
                    if (injectedValue != null) {
                        String injectedClassName = injectedValue.getClass().getName();
                        log.info("依赖注入验证: {}.{} -> {}", 
                            dependentBean.getClass().getSimpleName(), 
                            field.getName(), 
                            injectedClassName);
                            
                        // 检查是否还是补丁类型
                        if (injectedClassName.contains("Patch")) {
                            log.warn("⚠️ 注入的仍然是补丁类型: {}", injectedClassName);
                        } else {
                            log.info("✅ 注入已更新为原始类型: {}", injectedClassName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("验证依赖注入时出现异常: {}", e.getMessage());
        }
    }
    
    /**
     * 获取补丁Bean的名称（Spring自动注册时使用的名称）
     */
    private String getPatchBeanName(Class<?> patchClass) {
        // 优先使用@Service、@Component等注解指定的值
        if (patchClass.isAnnotationPresent(org.springframework.stereotype.Service.class)) {
            org.springframework.stereotype.Service serviceAnnotation = 
                patchClass.getAnnotation(org.springframework.stereotype.Service.class);
            if (StringUtils.hasText(serviceAnnotation.value())) {
                return serviceAnnotation.value();
            }
        }
        
        if (patchClass.isAnnotationPresent(Component.class)) {
            Component componentAnnotation =
                patchClass.getAnnotation(Component.class);
            if (StringUtils.hasText(componentAnnotation.value())) {
                return componentAnnotation.value();
            }
        }
        
        // 默认使用类名的小驼峰格式
        String className = patchClass.getSimpleName();
        return className.substring(0, 1).toLowerCase() + className.substring(1);
    }
    
    /**
     * 专用的补丁类加载器 - 支持真正的类卸载和重新加载
     */
    private static class PatchClassLoader extends URLClassLoader {
        private final Map<String, Class<?>> loadedClasses = new ConcurrentHashMap<>();
        
        public PatchClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }
        
        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                // 检查类是否已经在当前类加载器中加载
                Class<?> c = findLoadedClass(name);
                if (c == null) {
                    // 对于补丁相关的类，优先从当前类加载器加载，避免父类加载器的缓存
                    if (name.contains("Patch") || name.contains("patch")) {
                        try {
                            c = findClass(name);
                            loadedClasses.put(name, c);
                            log.debug("通过补丁类加载器加载类: {}", name);
                        } catch (ClassNotFoundException e) {
                            // 如果找不到，则委托给父类加载器
                            c = super.loadClass(name, false);
                        }
                    } else {
                        // 非补丁类，使用标准的双亲委派机制
                        c = super.loadClass(name, false);
                    }
                }
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            }
        }
        
        /**
         * 强制卸载指定的类
         */
        public void unloadClass(String className) {
            loadedClasses.remove(className);
            // 清理类加载器的内部缓存
            try {
                java.lang.reflect.Field classesField = ClassLoader.class.getDeclaredField("classes");
                classesField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(this);
                classes.removeIf(clazz -> clazz.getName().equals(className));
                log.debug("已从类加载器缓存中移除类: {}", className);
            } catch (Exception e) {
                log.debug("清理类加载器缓存失败: {}", e.getMessage());
            }
        }
        
        /**
         * 清理所有已加载的补丁类
         */
        public void clearPatchClasses() {
            Set<String> classesToRemove = new HashSet<>(loadedClasses.keySet());
            loadedClasses.clear();
            
            try {
                java.lang.reflect.Field classesField = ClassLoader.class.getDeclaredField("classes");
                classesField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(this);
                classes.removeIf(clazz -> clazz.getName().contains("Patch"));
                log.info("已清理补丁类缓存，移除类数量: {}", classesToRemove.size());
            } catch (Exception e) {
                log.debug("清理补丁类缓存失败: {}", e.getMessage());
            }
        }
        
        /**
         * 获取已加载的补丁类列表
         */
        public Set<String> getLoadedPatchClasses() {
            return new HashSet<>(loadedClasses.keySet());
        }
    }
    
    /**
     * 清理Spring的内部缓存
     */
    private void clearSpringInternalCaches(DefaultListableBeanFactory beanFactory, String beanName, Class<?> beanType) {
        try {
            // 清理单例缓存
            java.lang.reflect.Field singletonObjectsField = DefaultListableBeanFactory.class.getDeclaredField("singletonObjects");
            singletonObjectsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> singletonObjects = (Map<String, Object>) singletonObjectsField.get(beanFactory);
            singletonObjects.remove(beanName);
            
            // 清理早期单例对象缓存
            java.lang.reflect.Field earlySingletonObjectsField = DefaultListableBeanFactory.class.getDeclaredField("earlySingletonObjects");
            earlySingletonObjectsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> earlySingletonObjects = (Map<String, Object>) earlySingletonObjectsField.get(beanFactory);
            earlySingletonObjects.remove(beanName);
            
            // 清理单例工厂缓存
            java.lang.reflect.Field singletonFactoriesField = DefaultListableBeanFactory.class.getDeclaredField("singletonFactories");
            singletonFactoriesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ?> singletonFactories = (Map<String, ?>) singletonFactoriesField.get(beanFactory);
            singletonFactories.remove(beanName);
            
            // 清理类型缓存
            if (beanType != null) {
                java.lang.reflect.Field allBeanNamesByTypeField = DefaultListableBeanFactory.class.getDeclaredField("allBeanNamesByType");
                allBeanNamesByTypeField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<Class<?>, String[]> allBeanNamesByType = (Map<Class<?>, String[]>) allBeanNamesByTypeField.get(beanFactory);
                allBeanNamesByType.remove(beanType);
                
                java.lang.reflect.Field singletonBeanNamesByTypeField = DefaultListableBeanFactory.class.getDeclaredField("singletonBeanNamesByType");
                singletonBeanNamesByTypeField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<Class<?>, String[]> singletonBeanNamesByType = (Map<Class<?>, String[]>) singletonBeanNamesByTypeField.get(beanFactory);
                singletonBeanNamesByType.remove(beanType);
            }
            
            log.debug("已清理Spring内部缓存");
        } catch (Exception e) {
            log.warn("清理Spring内部缓存失败: {}", e.getMessage());
        }
    }
    
    /**
     * 清理可解析依赖映射
     */
    private void clearResolvableDependency(DefaultListableBeanFactory beanFactory, Class<?> dependencyType) {
        try {
            java.lang.reflect.Field resolvableDependenciesField = DefaultListableBeanFactory.class.getDeclaredField("resolvableDependencies");
            resolvableDependenciesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Class<?>, Object> resolvableDependencies = (Map<Class<?>, Object>) resolvableDependenciesField.get(beanFactory);
            resolvableDependencies.remove(dependencyType);
            log.debug("已清理可解析依赖映射: {}", dependencyType.getName());
        } catch (Exception e) {
            log.warn("清理可解析依赖映射失败: {}", e.getMessage());
        }
    }
    
    /**
     * 强制更新所有Bean中已注入的字段引用
     */
    private void updateInjectedFieldReferences(ConfigurableListableBeanFactory beanFactory,
                                               Class<?> targetType, Object newInstance) {
        try {
            log.info("开始更新所有Bean中的字段引用: {} -> {}", 
                targetType.getName(), newInstance.getClass().getName());
            
            // 获取所有Bean名称
            String[] allBeanNames = beanFactory.getBeanDefinitionNames();
            int updatedCount = 0;
            
            for (String beanName : allBeanNames) {
                try {
                    // 跳过目标Bean本身
                    if (beanFactory.containsBean(beanName)) {
                        Object bean = beanFactory.getSingleton(beanName);
                        if (bean != null && bean != newInstance) {
                            // 检查并更新该Bean中的字段
                            boolean updated = updateFieldsInBean(bean, targetType, newInstance);
                            if (updated) {
                                updatedCount++;
                                log.info("已更新Bean {} 中的字段引用", beanName);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("更新Bean {} 字段引用时出现异常: {}", beanName, e.getMessage());
                }
            }
            
            log.info("✅ 字段引用更新完成，共更新了 {} 个Bean", updatedCount);
            
        } catch (Exception e) {
            log.error("更新字段引用失败", e);
        }
    }
    
    /**
     * 更新单个Bean中的字段引用
     */
    private boolean updateFieldsInBean(Object bean, Class<?> targetType, Object newInstance) {
        boolean updated = false;
        
        try {
            Class<?> beanClass = bean.getClass();
            
            // 获取所有字段，包括继承的字段
            java.lang.reflect.Field[] fields = beanClass.getDeclaredFields();
            
            for (java.lang.reflect.Field field : fields) {
                try {
                    // 检查字段类型是否匹配
                    if (targetType.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        Object currentValue = field.get(bean);
                        
                        // 如果当前值不是新实例，则更新它
                        if (currentValue != null && currentValue != newInstance) {
                            field.set(bean, newInstance);
                            updated = true;
                            log.debug("更新字段 {}.{}: {} -> {}", 
                                beanClass.getSimpleName(), field.getName(),
                                currentValue.getClass().getName(), 
                                newInstance.getClass().getName());
                        }
                    }
                } catch (Exception e) {
                    log.debug("更新字段 {}.{} 时出现异常: {}", 
                        beanClass.getSimpleName(), field.getName(), e.getMessage());
                }
            }
            
            // 也检查父类的字段
            Class<?> superClass = beanClass.getSuperclass();
            while (superClass != null && superClass != Object.class) {
                java.lang.reflect.Field[] superFields = superClass.getDeclaredFields();
                for (java.lang.reflect.Field field : superFields) {
                    try {
                        if (targetType.isAssignableFrom(field.getType())) {
                            field.setAccessible(true);
                            Object currentValue = field.get(bean);
                            
                            if (currentValue != null && currentValue != newInstance) {
                                field.set(bean, newInstance);
                                updated = true;
                                log.debug("更新父类字段 {}.{}: {} -> {}", 
                                    superClass.getSimpleName(), field.getName(),
                                    currentValue.getClass().getName(), 
                                    newInstance.getClass().getName());
                            }
                        }
                    } catch (Exception e) {
                        log.debug("更新父类字段 {}.{} 时出现异常: {}", 
                            superClass.getSimpleName(), field.getName(), e.getMessage());
                    }
                }
                superClass = superClass.getSuperclass();
            }
            
        } catch (Exception e) {
            log.debug("检查Bean {} 字段时出现异常: {}", bean.getClass().getName(), e.getMessage());
        }
        
        return updated;
    }
    
    /**
     * 验证补丁加载是否成功
     */
    private void verifyPatchLoading(PatchInfo patchInfo) {
        try {
            HotPatch annotation = patchInfo.getPatchClass().getAnnotation(HotPatch.class);
            String beanName = annotation.originalBean();
            
            if (!StringUtils.hasText(beanName)) {
                log.warn("无法验证补丁加载：缺少Bean名称");
                return;
            }
            
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
            Class<?> originalBeanType = originalBeanTypes.get(beanName);
            
            log.info("🔍 验证补丁加载状态: {}", beanName);
            
            // 1. 验证Bean容器中的实例
            Object beanByName = beanFactory.getBean(beanName);
            log.info("按名称获取Bean: {} -> {}", beanName, beanByName.getClass().getName());
            
            if (originalBeanType != null) {
                Object beanByType = beanFactory.getBean(originalBeanType);
                log.info("按类型获取Bean: {} -> {}", originalBeanType.getName(), beanByType.getClass().getName());
                
                // 验证是否是补丁类型
                boolean isPatchType = beanByType.getClass().getName().contains("Patch");
                log.info("Bean类型验证: {} (是否为补丁类型: {})", 
                    beanByType.getClass().getName(), isPatchType);
                
                if (isPatchType) {
                    // 测试补丁功能
                    // testPatchFunctionality(beanByType, originalBeanType);
                } else {
                    log.warn("⚠️ 补丁加载验证失败：Bean类型不是补丁类型");
                }
            }
            
        } catch (Exception e) {
            log.error("验证补丁加载失败", e);
        }
    }
    
    /**
     * 测试补丁功能是否正常
     */
    private void testPatchFunctionality(Object bean, Class<?> beanType) {
        try {
            // 特殊处理 UserService 的测试
            if (beanType.getSimpleName().equals("UserService")) {
                java.lang.reflect.Method getUserInfoMethod = beanType.getMethod("getUserInfo", Long.class);
                Object result = getUserInfoMethod.invoke(bean, 3L);
                
                // 补丁版本 UserService 对于未知用户ID应该返回 "未知用户"
                if ("未知用户".equals(result)) {
                    log.info("✅ 补丁功能测试通过: 未知用户ID返回'未知用户'（补丁行为）");
                } else {
                    log.warn("⚠️ 补丁功能测试异常: 未知用户ID返回 '{}' 而非'未知用户'", result);
                }
            }
        } catch (Exception e) {
            log.debug("补丁功能测试失败: {}", e.getMessage());
        }
    }
    
    /**
     * 测试原始Bean的功能是否已恢复
     */
    private void testOriginalBeanFunctionality(Object bean, Class<?> beanType) {
        try {
            // 特殊处理 UserService 的测试
            if (beanType.getSimpleName().equals("UserService")) {
                java.lang.reflect.Method getUserInfoMethod = beanType.getMethod("getUserInfo", Long.class);
                Object result = getUserInfoMethod.invoke(bean, 3L);
                
                // 原始 UserService 对于未知用户ID应该返回 null
                if (result == null) {
                    log.info("✅ 原始Bean功能测试通过: 未知用户ID返回null（原始行为）");
                } else {
                    log.warn("⚠️ 原始Bean功能测试异常: 未知用户ID返回 '{}' 而非null", result);
                }
            }
        } catch (Exception e) {
            log.debug("原始Bean功能测试失败: {}", e.getMessage());
        }
    }
}