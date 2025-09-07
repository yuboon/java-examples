package com.example.onlinedebug.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;

/**
 * Online Debug Agent - Runtime bytecode injection tool
 * 
 * Usage:
 * java -javaagent:springboot-online-debug.jar -jar your-app.jar
 */
public class OnlineDebugAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(OnlineDebugAgent.class);
    
    private static Instrumentation instrumentation;
    
    /**
     * JVM启动时调用的premain方法
     */
    public static void premain(String args, Instrumentation inst) {
        logger.info("Online Debug Agent starting...");
        instrumentation = inst;
        installAgent(inst);
        logger.info("Online Debug Agent installed successfully");
    }
    
    /**
     * 运行时动态加载Agent时调用的agentmain方法
     */
    public static void agentmain(String args, Instrumentation inst) {
        logger.info("Online Debug Agent attaching at runtime...");
        instrumentation = inst;
        installAgent(inst);
        logger.info("Online Debug Agent attached successfully");
    }
    
    /**
     * 安装字节码增强
     */
    private static void installAgent(Instrumentation inst) {
        new AgentBuilder.Default()
            // 排除基础JVM类和可能导致问题的类
            .ignore(ElementMatchers.nameStartsWith("java."))
            .ignore(ElementMatchers.nameStartsWith("javax."))
            .ignore(ElementMatchers.nameStartsWith("sun."))
            .ignore(ElementMatchers.nameStartsWith("com.sun."))
            .ignore(ElementMatchers.nameStartsWith("jdk."))
            .ignore(ElementMatchers.nameStartsWith("org.springframework.core.SerializableTypeWrapper"))
            .ignore(ElementMatchers.nameStartsWith("org.springframework.cglib."))
            .ignore(ElementMatchers.nameStartsWith("org.springframework.boot.loader."))
            .ignore(ElementMatchers.nameStartsWith("org.springframework.boot.autoconfigure."))
            .ignore(ElementMatchers.nameStartsWith("org.apache.catalina."))
            .ignore(ElementMatchers.nameStartsWith("org.apache.tomcat."))
            .ignore(ElementMatchers.nameStartsWith("ch.qos.logback."))
            .ignore(ElementMatchers.nameStartsWith("net.bytebuddy."))
            .ignore(ElementMatchers.nameContains("CGLIB$$"))
            .ignore(ElementMatchers.nameContains("$$EnhancerBy"))
            .ignore(ElementMatchers.nameContains("$$FastClass"))
            .ignore(ElementMatchers.isSynthetic())
            
            // 动态匹配需要调试的类
            .type(new ElementMatcher<TypeDescription>() {
                @Override
                public boolean matches(TypeDescription target) {
                    String className = target.getName();
                    
                    // 默认包含的演示类
                    if (className.startsWith("com.example.onlinedebug.demo.")) {
                        return true;
                    }
                    
                    // 检查是否有动态配置的调试规则
                    return DebugConfigManager.shouldDebugClass(className);
                }
            })
            .transform(new AgentBuilder.Transformer() {
                @Override
                public net.bytebuddy.dynamic.DynamicType.Builder<?> transform(
                        net.bytebuddy.dynamic.DynamicType.Builder<?> builder, 
                        net.bytebuddy.description.type.TypeDescription typeDescription, 
                        ClassLoader classLoader, 
                        net.bytebuddy.utility.JavaModule module,
                        java.security.ProtectionDomain protectionDomain) {
                    return builder
                        .visit(Advice.to(UniversalDebugAdvice.class)
                            .on(ElementMatchers.any()
                                .and(ElementMatchers.not(ElementMatchers.isConstructor()))
                                .and(ElementMatchers.not(ElementMatchers.isStatic()))
                                .and(ElementMatchers.not(ElementMatchers.named("toString")))
                                .and(ElementMatchers.not(ElementMatchers.named("hashCode")))
                                .and(ElementMatchers.not(ElementMatchers.named("equals")))
                                .and(ElementMatchers.not(ElementMatchers.isSynthetic()))
                                .and(ElementMatchers.not(ElementMatchers.isBridge()))));
                }
            })
            .with(new AgentBuilder.Listener() {
                @Override
                public void onDiscovery(String typeName, ClassLoader classLoader, net.bytebuddy.utility.JavaModule module, boolean loaded) {
                    // 静默处理发现的类
                }
                
                @Override
                public void onTransformation(net.bytebuddy.description.type.TypeDescription typeDescription, 
                                           ClassLoader classLoader, 
                                           net.bytebuddy.utility.JavaModule module, 
                                           boolean loaded, 
                                           net.bytebuddy.dynamic.DynamicType dynamicType) {
                    // 记录转换的类
                    logger.debug("Transformed class: {}", typeDescription.getName());
                }
                
                @Override
                public void onIgnored(net.bytebuddy.description.type.TypeDescription typeDescription, 
                                    ClassLoader classLoader, 
                                    net.bytebuddy.utility.JavaModule module, 
                                    boolean loaded) {
                    // 静默处理忽略的类
                }
                
                @Override
                public void onError(String typeName, ClassLoader classLoader, net.bytebuddy.utility.JavaModule module, boolean loaded, Throwable throwable) {
                    logger.warn("Failed to transform class: {}", typeName, throwable);
                }
                
                @Override
                public void onComplete(String typeName, ClassLoader classLoader, net.bytebuddy.utility.JavaModule module, boolean loaded) {
                    // 静默处理完成的类
                }
            })
            .installOn(inst);
    }
    
    /**
     * 获取Instrumentation实例，供其他组件使用
     */
    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }
}