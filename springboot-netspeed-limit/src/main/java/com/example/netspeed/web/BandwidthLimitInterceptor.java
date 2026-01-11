package com.example.netspeed.web;

import com.example.netspeed.annotation.BandwidthLimit;
import com.example.netspeed.annotation.BandwidthUnit;
import com.example.netspeed.annotation.LimitType;
import com.example.netspeed.core.TokenBucket;
import com.example.netspeed.manager.BandwidthLimitManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 带宽限速拦截器
 *
 * 在 preHandle 中包装响应，在 afterCompletion 中关闭
 */
@Slf4j
public class BandwidthLimitInterceptor implements HandlerInterceptor {

    private final BandwidthLimitManager limitManager = new BandwidthLimitManager();

    private static final String WRAPPED_RESPONSE_ATTR = "BandwidthLimitWrappedResponse";
    private static final String ORIGINAL_RESPONSE_ATTR = "BandwidthLimitOriginalResponse";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        BandwidthLimit annotation = handlerMethod.getMethodAnnotation(BandwidthLimit.class);

        // 如果方法没有注解，检查类级别的注解
        if (annotation == null) {
            annotation = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), BandwidthLimit.class);
        }

        if (annotation != null) {
            String path = request.getRequestURI();
            log.info("========== Interceptor: Found @BandwidthLimit for path: {}, type: {}, value: {} {}/s ==========",
                path, annotation.type(), annotation.value(), annotation.unit());

            // 获取带宽参数
            LimitType type = annotation.type();
            long bandwidth = calculateBandwidth(request, annotation);
            long bandwidthBytesPerSecond = annotation.unit().toBytesPerSecond(bandwidth);
            long capacity = (long) (bandwidthBytesPerSecond * annotation.capacityMultiplier());
            String key = getLimitKey(request, type, path, annotation);

            // 获取或创建令牌桶
            TokenBucket bucket = limitManager.getBucket(type, key, capacity, bandwidthBytesPerSecond);

            log.info("Interceptor: Token bucket created - type={}, key={}, capacity={}/s, rate={}/s",
                type, key, BandwidthUnit.formatBytes(capacity), BandwidthUnit.formatBytes(bandwidthBytesPerSecond));

            // 设置响应头到原始响应（这样浏览器才能看到）
            response.setHeader("X-Bandwidth-Limit", BandwidthUnit.formatBytes(bandwidthBytesPerSecond) + "/s");
            response.setHeader("X-Bandwidth-Type", type.name());
            response.setHeader("X-Bandwidth-Key", key);
            response.setHeader("X-Bandwidth-Capacity", BandwidthUnit.formatBytes(capacity));

            log.info("Interceptor: Response headers set - X-Bandwidth-Limit={}",
                BandwidthUnit.formatBytes(bandwidthBytesPerSecond) + "/s");

            // 创建限速响应包装器（传入共享的 TokenBucket）
            BandwidthLimitResponseWrapper wrappedResponse = new BandwidthLimitResponseWrapper(
                response, bucket, bandwidthBytesPerSecond, annotation.chunkSize());

            // 将包装器保存到请求中
            request.setAttribute(WRAPPED_RESPONSE_ATTR, wrappedResponse);
            request.setAttribute(ORIGINAL_RESPONSE_ATTR, response);
            request.setAttribute("BandwidthLimit", annotation);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 清理资源
        BandwidthLimitResponseWrapper wrappedResponse =
            (BandwidthLimitResponseWrapper) request.getAttribute(WRAPPED_RESPONSE_ATTR);
        if (wrappedResponse != null) {
            try {
                wrappedResponse.close();
            } catch (Exception e) {
                log.error("Error closing wrapped response", e);
            }
        }
    }

    private long calculateBandwidth(HttpServletRequest request, BandwidthLimit annotation) {
        if (annotation.free() > 0 || annotation.vip() > 0) {
            String userType = request.getHeader("X-User-Type");
            if ("vip".equalsIgnoreCase(userType)) {
                return annotation.vip() > 0 ? annotation.vip() : annotation.value();
            } else if ("free".equalsIgnoreCase(userType)) {
                return annotation.free() > 0 ? annotation.free() : annotation.value();
            }
        }
        return annotation.value();
    }

    private String getLimitKey(HttpServletRequest request, LimitType type, String path, BandwidthLimit annotation) {
        return switch (type) {
            case GLOBAL -> "global";
            case API -> path;
            case USER -> {
                String userId = request.getHeader(annotation.userHeader());
                yield userId != null ? userId : request.getRemoteAddr();
            }
            case IP -> getClientIp(request);
        };
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    public BandwidthLimitManager.BandwidthLimitStats getStats() {
        return limitManager.getStats();
    }

    public void resetGlobalBucket() {
        limitManager.resetGlobalBucket();
    }

    public void clearAllBuckets() {
        limitManager.clearAllBuckets();
    }

    public void shutdown() {
        limitManager.shutdown();
    }
}
