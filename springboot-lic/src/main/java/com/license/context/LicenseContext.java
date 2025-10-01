package com.license.context;

import com.license.entity.License;

/**
 * 许可证上下文类
 * 用于在应用运行期间存储和访问当前有效的许可证信息
 */
public class LicenseContext {

    private static final ThreadLocal<License> licenseHolder = new ThreadLocal<>();
    private static License globalLicense;

    /**
     * 设置全局许可证（通常在应用启动时设置）
     */
    public static void setCurrentLicense(License license) {
        globalLicense = license;
    }

    /**
     * 获取当前许可证
     * 优先从ThreadLocal获取，如果没有则返回全局许可证
     */
    public static License getCurrentLicense() {
        License license = licenseHolder.get();
        return license != null ? license : globalLicense;
    }

    /**
     * 为当前线程设置许可证（用于特殊场景）
     */
    public static void setThreadLocalLicense(License license) {
        licenseHolder.set(license);
    }

    /**
     * 清除当前线程的许可证
     */
    public static void clearThreadLocalLicense() {
        licenseHolder.remove();
    }

    /**
     * 检查是否有指定的功能权限
     */
    public static boolean hasFeature(String feature) {
        License license = getCurrentLicense();
        if (license == null || license.getFeatures() == null) {
            return false;
        }
        return license.getFeatures().contains(feature);
    }

    /**
     * 清空全局许可证（谨慎使用）
     */
    public static void clearGlobalLicense() {
        globalLicense = null;
    }
}
