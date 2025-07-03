package com.example.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MonitorAgent {

    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public static void premain(String arguments, Instrumentation instrumentation) {
        System.out.println("SpringBoot监控Agent已启动...");
        log();
        // 使用ByteBuddy拦截SpringBoot的Controller方法
        new AgentBuilder.Default()
            .type(ElementMatchers.nameEndsWith("Controller"))
            .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                builder.method(ElementMatchers.isAnnotatedWith(
                        ElementMatchers.named("org.springframework.web.bind.annotation.RequestMapping")
                        .or(ElementMatchers.named("org.springframework.web.bind.annotation.GetMapping"))
                        .or(ElementMatchers.named("org.springframework.web.bind.annotation.PostMapping"))
                        .or(ElementMatchers.named("org.springframework.web.bind.annotation.PutMapping"))
                        .or(ElementMatchers.named("org.springframework.web.bind.annotation.DeleteMapping"))
                    ))
                    .intercept(MethodDelegation.to(ControllerInterceptor.class))
            )
            .installOn(instrumentation);
    }

    private static void log(){
        executorService.scheduleAtFixedRate(() -> {
            MetricsCollector.monitorJvmMetrics();
            // 收集并打印性能指标
            String text = MetricsCollector.scrape();
            System.out.println("===============");
            System.out.println(text);
        }, 0, 5, TimeUnit.SECONDS);
    }
}