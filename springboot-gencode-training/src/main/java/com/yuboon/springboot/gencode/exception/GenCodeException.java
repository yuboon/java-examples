package com.yuboon.springboot.gencode.exception;

import javax.management.RuntimeMBeanException;

/**
 * 此处为类介绍
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/08
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
