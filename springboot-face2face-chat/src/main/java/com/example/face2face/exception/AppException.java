package com.example.face2face.exception;

public class AppException extends RuntimeException {
    private int code;

    public AppException(String message) {
        super(message);
        this.code = 500;
    }

    public AppException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}