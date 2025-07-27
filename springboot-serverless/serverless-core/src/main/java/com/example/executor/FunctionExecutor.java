package com.example.executor;

import com.example.model.ExecutionContext;
import com.example.model.ExecutionResult;
import com.example.model.ServerlessFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 函数执行器
 * 负责在隔离环境中执行函数
 */
@Component
@Slf4j
public class FunctionExecutor {
    
    private final ExecutorService executorService;
    
    public FunctionExecutor() {
        // 创建线程池用于执行函数
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setName("function-executor-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * 执行函数
     */
    public ExecutionResult execute(String functionName, String jarPath, String className, 
                                 Map<String, Object> input, ExecutionContext context) {
        
        ExecutionResult result = new ExecutionResult(context.getRequestId(), functionName);

        Future<Object> future = executorService.submit(() -> {
            IsolatedClassLoader classLoader = null;
            try {
                // 创建隔离的类加载器
                URL jarUrl = new File(jarPath).toURI().toURL();
                classLoader = new IsolatedClassLoader(
                    functionName, 
                    new URL[]{jarUrl}, 
                    Thread.currentThread().getContextClassLoader()
                );
                
                // 加载函数类
                Class<?> functionClass = classLoader.loadClass(className);
                Object functionInstance = functionClass.getDeclaredConstructor().newInstance();
                
                // 检查是否实现了ServerlessFunction接口
                if (!(functionInstance instanceof ServerlessFunction)) {
                    throw new IllegalArgumentException(
                        "Function class must implement ServerlessFunction interface");
                }
                
                ServerlessFunction function = (ServerlessFunction) functionInstance;
                // 执行函数
                return function.handle(input, context);
                
            } finally {
                if (classLoader != null) {
                    try {
                        classLoader.close();
                    } catch (Exception e) {
                        // 忽略关闭异常
                    }
                }
            }
        });
        
        try {
            // 等待执行结果，支持超时
            Object functionResult = future.get(context.getTimeoutMs(), TimeUnit.MILLISECONDS);
            result.markSuccess(functionResult);
        } catch (TimeoutException e) {
            future.cancel(true);
            result.markFailure("TIMEOUT", "Function execution timeout");
            
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            log.error(cause.getMessage(),cause);
            result.markFailure(
                cause.getClass().getSimpleName(), 
                cause.getMessage()
            );
            
        } catch (Exception e) {
            result.markFailure(
                e.getClass().getSimpleName(), 
                e.getMessage()
            );
        }

        return result;
    }
    
    /**
     * 关闭执行器
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}