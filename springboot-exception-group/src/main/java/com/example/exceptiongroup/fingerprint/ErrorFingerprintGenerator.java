package com.example.exceptiongroup.fingerprint;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 错误指纹生成器
 * 基于异常类型、发生位置、调用链生成唯一指纹
 */
@Component
public class ErrorFingerprintGenerator {

    /**
     * 生成错误指纹
     */
    public String generateFingerprint(Throwable throwable) {
        if (throwable == null) {
            return "";
        }

        // 获取异常发生的根本位置
        StackTraceElement rootCause = getRootCauseLocation(throwable);
        if (rootCause == null) {
            return DigestUtils.md5Hex(throwable.getClass().getSimpleName());
        }

        StringBuilder fingerprint = new StringBuilder()
            .append(throwable.getClass().getSimpleName())
            .append("|")
            .append(rootCause.getClassName())
            .append("#")
            .append(rootCause.getMethodName())
            .append(":")
            .append(rootCause.getLineNumber());

        // 过滤异常消息中的动态值，保留结构特征
        String filteredMessage = filterDynamicValues(throwable.getMessage());
        if (StringUtils.isNotBlank(filteredMessage)) {
            fingerprint.append("|").append(filteredMessage);
        }

        // 注释掉调用链哈希，避免相同错误产生不同指纹
        // String callChainHash = getCallChainHash(throwable.getStackTrace(), 3);
        // if (StringUtils.isNotBlank(callChainHash)) {
        //     fingerprint.append("|").append(callChainHash);
        // }

        return DigestUtils.md5Hex(fingerprint.toString());
    }

    /**
     * 获取异常的根本发生位置
     */
    private StackTraceElement getRootCauseLocation(Throwable throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace == null || stackTrace.length == 0) {
            return null;
        }

        // 找到第一个非框架代码的位置
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (!isFrameworkClass(className)) {
                return element;
            }
        }

        // 如果都是框架代码，返回第一个
        return stackTrace[0];
    }

    /**
     * 判断是否为框架代码
     */
    private boolean isFrameworkClass(String className) {
        return className.startsWith("java.") ||
               className.startsWith("javax.") ||
               className.startsWith("org.springframework.") ||
               className.startsWith("org.apache.") ||
               className.startsWith("com.sun.");
    }

    /**
     * 过滤消息中的动态值
     */
    private String filterDynamicValues(String message) {
        if (message == null) {
            return "";
        }

        return message
            // 过滤数字ID
            .replaceAll("\\b\\d{4,}\\b", "NUM")
            // 过滤UUID
            .replaceAll("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b", "UUID")
            // 过滤时间戳
            .replaceAll("\\b\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\b", "TIMESTAMP")
            // 过滤IP地址
            .replaceAll("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b", "IP")
            // 限制长度
            .substring(0, Math.min(message.length(), 100));
    }

    /**
     * 生成调用链特征哈希
     */
    private String getCallChainHash(StackTraceElement[] stackTrace, int depth) {
        if (stackTrace == null || stackTrace.length == 0) {
            return "";
        }

        StringBuilder callChain = new StringBuilder();
        int actualDepth = Math.min(depth, stackTrace.length);

        for (int i = 0; i < actualDepth; i++) {
            StackTraceElement element = stackTrace[i];
            callChain.append(element.getClassName())
                    .append("#")
                    .append(element.getMethodName())
                    .append("|");
        }

        return callChain.length() > 0 ?
            DigestUtils.md5Hex(callChain.toString()).substring(0, 8) : "";
    }
}