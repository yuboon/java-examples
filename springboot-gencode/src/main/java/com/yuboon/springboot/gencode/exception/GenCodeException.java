package com.yuboon.springboot.gencode.exception;

/**
 * 代码生成异常类
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/04
 */
public class GenCodeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public GenCodeException() {
        super();
    }

    public GenCodeException(String message, Throwable cause) {
        super(message, cause);
    }

    public GenCodeException(String message) {
        super(message);
    }

}
