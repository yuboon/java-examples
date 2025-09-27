package com.example.exceptiongroup.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.Encoder;
import com.example.exceptiongroup.cache.ErrorFingerprintCache;
import com.example.exceptiongroup.fingerprint.ErrorFingerprintGenerator;
import com.example.exceptiongroup.util.TraceIdGenerator;

/**
 * 自定义Logback Appender
 * 实现错误指纹聚类和智能日志输出
 */
public class ErrorFingerprintAppender extends AppenderBase<ILoggingEvent> {

    private ErrorFingerprintGenerator fingerprintGenerator;
    private ErrorFingerprintCache fingerprintCache;
    private TraceIdGenerator traceIdGenerator;

    // 委托给控制台输出
    private ConsoleAppender<ILoggingEvent> consoleAppender;
    private Encoder<ILoggingEvent> encoder;

    @Override
    public void start() {
        // 使用单例实例，确保与Spring容器中的Bean是同一个
        this.fingerprintGenerator = new ErrorFingerprintGenerator();
        this.fingerprintCache = ErrorFingerprintCache.getInstance();
        this.traceIdGenerator = new TraceIdGenerator();

        System.out.println("=== ErrorFingerprintAppender 启动 ===");
        System.out.println("Cache instance: " + this.fingerprintCache);

        // 初始化控制台输出
        this.consoleAppender = new ConsoleAppender<>();
        this.consoleAppender.setContext(getContext());
        this.consoleAppender.setName("console-delegate");

        if (encoder != null) {
            this.consoleAppender.setEncoder(encoder);
        }

        this.consoleAppender.start();
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted()) {
            return;
        }

        // 生成或获取TraceId
        String traceId = traceIdGenerator.getCurrentTraceId();

        System.out.println("=== Appender处理日志事件 ===");
        System.out.println("Level: " + event.getLevel());
        System.out.println("Message: " + event.getMessage());
        System.out.println("HasException: " + (event.getThrowableProxy() != null));

        if (isErrorEvent(event)) {
            System.out.println("处理错误事件...");
            handleErrorEvent(event, traceId);
        } else {
            // 非错误日志直接输出
            consoleAppender.doAppend(event);
        }
    }

    /**
     * 处理错误事件
     */
    private void handleErrorEvent(ILoggingEvent event, String traceId) {
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy == null) {
            consoleAppender.doAppend(event);
            return;
        }

        // 转换为Throwable对象用于指纹生成
        Throwable throwable = convertToThrowable(throwableProxy);
        if (throwable == null) {
            consoleAppender.doAppend(event);
            return;
        }

        // 生成错误指纹
        String fingerprint = fingerprintGenerator.generateFingerprint(throwable);

        // 检查是否应该输出日志
        if (fingerprintCache.shouldLog(fingerprint, traceId)) {
            // 创建增强的日志事件，包含指纹和统计信息
            ErrorFingerprintCache.ErrorFingerprint fpInfo = fingerprintCache.getFingerprint(fingerprint);
            ILoggingEvent enhancedEvent = createEnhancedEvent(event, fingerprint, fpInfo, traceId);
            consoleAppender.doAppend(enhancedEvent);
        }
        // 不输出的情况：已经记录过且未达到阈值
    }

    /**
     * 判断是否为错误事件
     */
    private boolean isErrorEvent(ILoggingEvent event) {
        return event.getThrowableProxy() != null;
    }

    /**
     * 转换IThrowableProxy为Throwable
     */
    private Throwable convertToThrowable(IThrowableProxy throwableProxy) {
        try {
            String className = throwableProxy.getClassName();
            String message = throwableProxy.getMessage();

            // 创建异常实例
            Class<?> exceptionClass = Class.forName(className);
            Throwable throwable = (Throwable) exceptionClass.getDeclaredConstructor(String.class).newInstance(message);

            // 设置堆栈轨迹
            StackTraceElementProxy[] proxyArray = throwableProxy.getStackTraceElementProxyArray();
            if (proxyArray != null) {
                StackTraceElement[] stackTrace = new StackTraceElement[proxyArray.length];
                for (int i = 0; i < proxyArray.length; i++) {
                    stackTrace[i] = proxyArray[i].getStackTraceElement();
                }
                throwable.setStackTrace(stackTrace);
            }

            return throwable;
        } catch (Exception e) {
            // 如果转换失败，创建一个通用异常
            RuntimeException genericException = new RuntimeException(throwableProxy.getMessage());
            return genericException;
        }
    }

    /**
     * 创建增强的日志事件
     */
    private ILoggingEvent createEnhancedEvent(ILoggingEvent originalEvent, String fingerprint,
                                             ErrorFingerprintCache.ErrorFingerprint fpInfo, String traceId) {
        // 创建包装的日志事件，添加指纹信息
        return new EnhancedLoggingEvent(originalEvent, fingerprint, fpInfo, traceId);
    }

    // Setter for encoder
    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }

    @Override
    public void stop() {
        if (consoleAppender != null) {
            consoleAppender.stop();
        }
        super.stop();
    }
}