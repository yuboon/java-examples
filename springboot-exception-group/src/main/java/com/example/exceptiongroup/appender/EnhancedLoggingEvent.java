package com.example.exceptiongroup.appender;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import com.example.exceptiongroup.cache.ErrorFingerprintCache;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import java.util.List;
import java.util.Map;

/**
 * 增强的日志事件，包含指纹信息
 */
public class EnhancedLoggingEvent implements ILoggingEvent {

    private final ILoggingEvent originalEvent;
    private final String fingerprint;
    private final ErrorFingerprintCache.ErrorFingerprint fpInfo;
    private final String traceId;
    private final String enhancedMessage;

    public EnhancedLoggingEvent(ILoggingEvent originalEvent, String fingerprint,
                               ErrorFingerprintCache.ErrorFingerprint fpInfo, String traceId) {
        this.originalEvent = originalEvent;
        this.fingerprint = fingerprint;
        this.fpInfo = fpInfo;
        this.traceId = traceId;
        this.enhancedMessage = buildEnhancedMessage();
    }

    private String buildEnhancedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[FINGERPRINT:").append(fingerprint.substring(0, 8)).append("]");
        sb.append("[COUNT:").append(fpInfo.getCount()).append("]");
        sb.append("[TRACE:").append(traceId).append("]");
        sb.append(" ").append(originalEvent.getFormattedMessage());

        if (fpInfo.getCount() > 1) {
            sb.append(" [SIMILAR_ERRORS:").append(fpInfo.getCount()).append("]");
            sb.append("[FIRST_SEEN:").append(fpInfo.getFirstOccurrence()).append("]");
        }

        return sb.toString();
    }

    @Override
    public String getFormattedMessage() {
        return enhancedMessage;
    }

    @Override
    public String getMessage() {
        return enhancedMessage;
    }

    // 委托给原始事件的方法
    @Override
    public String getThreadName() {
        return originalEvent.getThreadName();
    }

    @Override
    public Level getLevel() {
        return originalEvent.getLevel();
    }

    @Override
    public String getLoggerName() {
        return originalEvent.getLoggerName();
    }

    @Override
    public LoggerContextVO getLoggerContextVO() {
        return originalEvent.getLoggerContextVO();
    }

    @Override
    public IThrowableProxy getThrowableProxy() {
        return originalEvent.getThrowableProxy();
    }

    @Override
    public Object[] getArgumentArray() {
        return originalEvent.getArgumentArray();
    }

    @Override
    public long getTimeStamp() {
        return originalEvent.getTimeStamp();
    }

    @Override
    public Marker getMarker() {
        return originalEvent.getMarker();
    }

    @Override
    public List<Marker> getMarkerList() {
        return originalEvent.getMarkerList();
    }

    @Override
    public Map<String, String> getMDCPropertyMap() {
        return originalEvent.getMDCPropertyMap();
    }

    @Override
    public Map<String, String> getMdc() {
        return originalEvent.getMdc();
    }

    @Override
    public void prepareForDeferredProcessing() {
        originalEvent.prepareForDeferredProcessing();
    }

    // 新版本Logback接口的额外方法实现
    @Override
    public List<KeyValuePair> getKeyValuePairs() {
        return originalEvent.getKeyValuePairs();
    }

    @Override
    public int getNanoseconds() {
        return originalEvent.getNanoseconds();
    }

    @Override
    public long getSequenceNumber() {
        return originalEvent.getSequenceNumber();
    }

    @Override
    public boolean hasCallerData() {
        return originalEvent.hasCallerData();
    }

    @Override
    public StackTraceElement[] getCallerData() {
        return originalEvent.getCallerData();
    }
}