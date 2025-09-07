package com.example.onlinedebug.agent;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 通用调试拦截器
 * 在方法执行前后注入调试逻辑
 */
public class UniversalDebugAdvice {
    
    /**
     * 方法进入时执行
     */
    @Advice.OnMethodEnter
    public static long onEnter(@Advice.Origin Method method, 
                              @Advice.AllArguments Object[] args) {
        
        try {
            String className = method.getDeclaringClass().getName();
            String methodName = method.getName();
            String fullMethodName = className + "." + methodName;
            
            // 检查是否需要调试这个方法（恢复正常的动态逻辑）
            if (DebugConfigManager.shouldDebug(fullMethodName)) {
                long startTime = System.currentTimeMillis();
                
                StringBuilder logMessage = new StringBuilder();
                logMessage.append("[DEBUG-INJECT] ")
                          .append(fullMethodName)
                          .append("() called");
                
                // 添加参数信息
                if (args != null && args.length > 0) {
                    try {
                        logMessage.append(" with args: ");
                        for (int i = 0; i < args.length; i++) {
                            if (i > 0) logMessage.append(", ");
                            if (args[i] == null) {
                                logMessage.append("null");
                            } else {
                                String argStr = safeToString(args[i]);
                                logMessage.append(args[i].getClass().getSimpleName())
                                          .append("@")
                                          .append(argStr);
                            }
                        }
                    } catch (Exception e) {
                        logMessage.append(" [failed to serialize args: ").append(e.getMessage()).append("]");
                    }
                }
                
                System.out.println(logMessage.toString());
                return startTime;
            }
            
        } catch (Exception e) {
            System.err.println("[DEBUG-ADVICE] Error in onEnter: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0; // 不调试时返回0
    }
    
    /**
     * 方法退出时执行
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Origin Method method,
                             @Advice.Enter long startTime,
                             @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returnValue,
                             @Advice.Thrown Throwable throwable) {
        
        // 只有在进入时记录了开始时间的情况下才处理退出逻辑
        if (startTime > 0) {
            try {
                String className = method.getDeclaringClass().getName();
                String methodName = method.getName();
                String fullMethodName = className + "." + methodName;
                
                long duration = System.currentTimeMillis() - startTime;
                
                StringBuilder logMessage = new StringBuilder();
                logMessage.append("[DEBUG-INJECT] ")
                          .append(fullMethodName)
                          .append("() completed in ")
                          .append(duration)
                          .append("ms");
                
                // 处理返回值
                if (throwable != null) {
                    logMessage.append(" with exception: ")
                              .append(throwable.getClass().getSimpleName())
                              .append(": ")
                              .append(throwable.getMessage());
                } else if (!"void".equals(method.getReturnType().getName())) {
                    try {
                        if (returnValue == null) {
                            logMessage.append(" returning: null");
                        } else {
                            String returnStr = safeToString(returnValue);
                            logMessage.append(" returning: ")
                                      .append(returnValue.getClass().getSimpleName())
                                      .append("@")
                                      .append(returnStr);
                        }
                    } catch (Exception e) {
                        logMessage.append(" [failed to serialize return value: ").append(e.getMessage()).append("]");
                    }
                }
                
                System.out.println(logMessage.toString());
            } catch (Exception e) {
                // 静默处理异常，避免影响正常业务
            }
        }
    }
    
    /**
     * 安全的toString方法，避免递归和过长输出
     * 改为 public static 以供 ByteBuddy 生成的代码访问
     */
    public static String safeToString(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        try {
            String str = obj.toString();
            if (str.length() > 100) {
                return str.substring(0, 100) + "...";
            }
            return str;
        } catch (Exception e) {
            return obj.getClass().getSimpleName() + "@" + System.identityHashCode(obj);
        }
    }
}