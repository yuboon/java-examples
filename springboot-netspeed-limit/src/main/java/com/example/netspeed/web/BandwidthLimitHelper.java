package com.example.netspeed.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 带宽限速辅助类
 *
 * 用于从请求中获取限速响应包装器
 */
public class BandwidthLimitHelper {

    private static final String WRAPPED_RESPONSE_ATTR = "BandwidthLimitWrappedResponse";

    /**
     * 获取限速响应包装器（如果存在）
     */
    public static HttpServletResponse getLimitedResponse(HttpServletRequest request, HttpServletResponse defaultResponse) {
        BandwidthLimitResponseWrapper wrappedResponse =
            (BandwidthLimitResponseWrapper) request.getAttribute(WRAPPED_RESPONSE_ATTR);

        if (wrappedResponse != null) {
            return wrappedResponse;
        }

        return defaultResponse;
    }

    /**
     * 检查是否应用了限速
     */
    public static boolean isLimited(HttpServletRequest request) {
        return request.getAttribute(WRAPPED_RESPONSE_ATTR) != null;
    }
}
