package com.license.exception;

/**
 * 许可证异常类
 * 用于处理许可证相关的业务异常
 */
public class LicenseException extends RuntimeException {

    public LicenseException(String message) {
        super(message);
    }

    public LicenseException(String message, Throwable cause) {
        super(message, cause);
    }
}
