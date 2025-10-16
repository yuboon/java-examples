package com.example.asn1.exception;

/**
 * ASN.1解析异常类
 *
 * 
 * @version 1.0.0
 */
public class Asn1ParseException extends RuntimeException {

    /**
     * 错误代码
     */
    private final String errorCode;

    /**
     * 默认构造函数
     *
     * @param message 错误消息
     */
    public Asn1ParseException(String message) {
        super(message);
        this.errorCode = "ASN1_PARSE_ERROR";
    }

    /**
     * 带异常原因的构造函数
     *
     * @param message 错误消息
     * @param cause   异常原因
     */
    public Asn1ParseException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "ASN1_PARSE_ERROR";
    }

    /**
     * 自定义错误代码的构造函数
     *
     * @param errorCode 错误代码
     * @param message   错误消息
     */
    public Asn1ParseException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误代码
     *
     * @return 错误代码
     */
    public String getErrorCode() {
        return errorCode;
    }
}