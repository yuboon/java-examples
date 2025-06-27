package com.example.version;

import cn.hutool.core.date.DateUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;


/**
 * API版本请求条件
 */
public class ApiVersionRequestCondition implements RequestCondition<ApiVersionRequestCondition> {
    
    private final String apiVersion;

    private final HandlerMethod handlerMethod;

    private static final Random RANDOM = new SecureRandom();

    public ApiVersionRequestCondition(String apiVersion, HandlerMethod handlerMethod) {
        this.apiVersion = apiVersion;
        this.handlerMethod = handlerMethod;
    }


    @Override
    public ApiVersionRequestCondition combine(ApiVersionRequestCondition other) {
        // 采用方法上的版本号优先于类上的版本号
        return new ApiVersionRequestCondition(other.getApiVersion(),null);
    }
    
    @Override
    public ApiVersionRequestCondition getMatchingCondition(HttpServletRequest request) {
        //String requestVersion = VersionContextHolder.getVersion();
        String requestVersion = getVersion(request);
        
        // 版本比较逻辑，这里简化处理，只做字符串比较
        // 实际应用中可能需要更复杂的版本比较算法
        if (requestVersion.equals(this.apiVersion)) {
            return this;
        }
        
        return null;
    }
    
    @Override
    public int compareTo(ApiVersionRequestCondition other, HttpServletRequest request) {
        // 版本号越大优先级越高
        return other.getApiVersion().compareTo(this.apiVersion);
    }
    
    public String getApiVersion() {
        return apiVersion;
    }

    private String getVersion(HttpServletRequest request){
        // 获取客户端请求的版本
        String clientVersion = request.getHeader("Api-Version");
        if (clientVersion == null || clientVersion.isEmpty()) {
            // 如果客户端未指定版本，也可以从请求参数中获取
            clientVersion = request.getParameter("version");
        }

        // 如果客户端仍未指定版本，则应用灰度规则
        if (clientVersion == null || clientVersion.isEmpty()) {
            // 从请求中提取用户信息
            UserInfo userInfo = extractUserInfo(request);

            // 获取方法或类上的灰度发布注解
            GrayRelease grayRelease = handlerMethod.getMethodAnnotation(GrayRelease.class);
            if (grayRelease == null) {
                grayRelease = handlerMethod.getBeanType().getAnnotation(GrayRelease.class);
            }

            if (grayRelease != null) {
                // 应用灰度规则决定使用哪个版本
                clientVersion = applyGrayReleaseRules(grayRelease, userInfo);
            } else {
                // 默认使用最新版本
                ApiVersion apiVersion = handlerMethod.getMethodAnnotation(ApiVersion.class);
                if (apiVersion == null) {
                    apiVersion = handlerMethod.getBeanType().getAnnotation(ApiVersion.class);
                }

                clientVersion = apiVersion != null ? apiVersion.value() : "1.0";
            }
        }

        return clientVersion;
    }

    private UserInfo extractUserInfo(HttpServletRequest request) {
        UserInfo userInfo = new UserInfo();

        // 实际应用中这里可能从请求头、Cookie或JWT Token中提取用户信息
        // 这里仅作示例
        String userId = request.getHeader("User-Id");
        userInfo.setUserId(userId);

        String groups = request.getHeader("User-Groups");
        if (groups != null && !groups.isEmpty()) {
            userInfo.setGroups(groups.split(","));
        }

        String region = request.getHeader("User-Region");
        userInfo.setRegion(region);

        return userInfo;
    }

    /**
     * 应用灰度规则
     */
    private String applyGrayReleaseRules(GrayRelease grayRelease, UserInfo userInfo) {
        // 检查时间范围
        if (!grayRelease.startTime().isEmpty() && !grayRelease.endTime().isEmpty()) {
            try {
                Date now = new Date();
                Date startTime = DateUtil.parse(grayRelease.startTime());
                Date endTime = DateUtil.parse(grayRelease.endTime());

                if (now.before(startTime) || now.after(endTime)) {
                    return "1.0"; // 不在灰度时间范围内，使用旧版本
                }
            } catch (Exception e) {
                // 解析日期出错，忽略时间规则
            }
        }

        // 检查用户ID白名单
        if (!grayRelease.userIds().isEmpty() && userInfo.getUserId() != null) {
            String[] whitelistIds = grayRelease.userIds().split(",");
            if (Arrays.asList(whitelistIds).contains(userInfo.getUserId())) {
                return "2.0"; // 用户在白名单中，使用新版本
            }
        }

        // 检查用户组
        if (grayRelease.userGroups().length > 0 && userInfo.getGroups() != null) {
            for (String requiredGroup : grayRelease.userGroups()) {
                for (String userGroup : userInfo.getGroups()) {
                    if (requiredGroup.equals(userGroup)) {
                        return "2.0"; // 用户在指定组中，使用新版本
                    }
                }
            }
        }

        // 检查地区
        if (grayRelease.regions().length > 0 && userInfo.getRegion() != null) {
            for (String region : grayRelease.regions()) {
                if (region.equals(userInfo.getRegion())) {
                    return "2.0"; // 用户在指定地区，使用新版本
                }
            }
        }

        // 应用百分比规则
        if (grayRelease.percentage() > 0) {
            int randomValue = RANDOM.nextInt(100) + 1; // 1-100的随机数
            if (randomValue <= grayRelease.percentage()) {
                return "2.0"; // 随机命中百分比，使用新版本
            }
        }

        // 默认使用旧版本
        return "1.0";
    }

}