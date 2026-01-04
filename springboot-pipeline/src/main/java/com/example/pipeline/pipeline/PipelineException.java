package com.example.pipeline.pipeline;

/**
 * 管道执行异常
 */
public class PipelineException extends Exception {

    private final String nodeName;

    public PipelineException(String nodeName, String message) {
        super(message);
        this.nodeName = nodeName;
    }

    public PipelineException(String nodeName, String message, Throwable cause) {
        super(message, cause);
        this.nodeName = nodeName;
    }

    public String getNodeName() {
        return nodeName;
    }
}
