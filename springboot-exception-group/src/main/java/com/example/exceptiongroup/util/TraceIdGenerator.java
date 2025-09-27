package com.example.exceptiongroup.util;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * TraceId生成器
 * 用于生成和管理链路追踪ID
 */
@Component
public class TraceIdGenerator {

    private static final String TRACE_ID_KEY = "traceId";

    /**
     * 生成新的TraceId
     */
    public String generateTraceId() {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put(TRACE_ID_KEY, traceId);
        return traceId;
    }

    /**
     * 获取当前TraceId，如果不存在则生成新的
     */
    public String getCurrentTraceId() {
        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
        }
        return traceId;
    }

    /**
     * 清除当前TraceId
     */
    public void clearTraceId() {
        MDC.remove(TRACE_ID_KEY);
    }
}