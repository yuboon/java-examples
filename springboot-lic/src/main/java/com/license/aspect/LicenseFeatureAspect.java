package com.license.aspect;

import com.license.annotation.RequireFeature;
import com.license.context.LicenseContext;
import com.license.entity.License;
import com.license.exception.LicenseException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 许可证功能权限AOP切面
 * 用于拦截带有@RequireFeature注解的方法，检查功能权限
 */
@Component
@Aspect
@Order(1)
public class LicenseFeatureAspect {

    private static final Logger logger = LoggerFactory.getLogger(LicenseFeatureAspect.class);

    /**
     * 环绕通知：在方法执行前检查功能权限
     */
    @Around("@annotation(requireFeature)")
    public Object checkFeaturePermission(ProceedingJoinPoint joinPoint, RequireFeature requireFeature) throws Throwable {

        // 获取当前许可证信息
        License currentLicense = LicenseContext.getCurrentLicense();

        if (currentLicense == null) {
            logger.warn("访问需要授权的功能，但未找到有效许可证: {}", requireFeature.value());
            throw new LicenseException("系统未找到有效许可证，请联系管理员");
        }

        // 检查功能权限
        if (currentLicense.getFeatures() == null ||
            !currentLicense.getFeatures().contains(requireFeature.value())) {

            logger.warn("功能权限不足 - 用户: {}, 需要权限: {}, 拥有权限: {}",
                       currentLicense.getIssuedTo(),
                       requireFeature.value(),
                       currentLicense.getFeatures());

            throw new LicenseException(requireFeature.message() + ": " + requireFeature.value());
        }

        logger.debug("功能权限验证通过: {}", requireFeature.value());
        return joinPoint.proceed();
    }
}
