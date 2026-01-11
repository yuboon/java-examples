package com.example.netspeed.core;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 限速输出流（支持分块写入）
 *
 * 使用令牌桶算法控制写入速率，实现精确的带宽限速
 */
@Slf4j
public class RateLimitedOutputStream extends ServletOutputStream {

    private final OutputStream outputStream;
    private final TokenBucket tokenBucket;
    private final int chunkSize;
    private final long bandwidthBytesPerSecond;

    // 统计信息
    private long totalBytesWritten = 0;
    private final long startTime = System.nanoTime();
    private volatile boolean closed = false;
    private boolean logged = false;

    public RateLimitedOutputStream(OutputStream outputStream, long bandwidthBytesPerSecond) {
        this(outputStream, bandwidthBytesPerSecond, calculateOptimalChunkSize(bandwidthBytesPerSecond));
    }

    /**
     * 使用已有的 TokenBucket（共享限速状态）
     *
     * @param outputStream           底层输出流
     * @param tokenBucket            共享的令牌桶
     * @param bandwidthBytesPerSecond 限速（字节/秒）
     */
    public RateLimitedOutputStream(OutputStream outputStream,
                                   TokenBucket tokenBucket,
                                   long bandwidthBytesPerSecond) {
        this(outputStream, tokenBucket, bandwidthBytesPerSecond, calculateOptimalChunkSize(bandwidthBytesPerSecond));
    }

    /**
     * 使用已有的 TokenBucket（共享限速状态），指定分块大小
     *
     * @param outputStream           底层输出流
     * @param tokenBucket            共享的令牌桶
     * @param bandwidthBytesPerSecond 限速（字节/秒）
     * @param chunkSize              分块大小
     */
    public RateLimitedOutputStream(OutputStream outputStream,
                                   TokenBucket tokenBucket,
                                   long bandwidthBytesPerSecond,
                                   int chunkSize) {
        this.outputStream = outputStream;
        this.bandwidthBytesPerSecond = bandwidthBytesPerSecond;
        this.chunkSize = Math.max(512, Math.min(chunkSize, 65536));
        this.tokenBucket = tokenBucket;

        log.info("RateLimitedOutputStream created with shared bucket: bandwidth={}/s, chunkSize={}",
            formatBytes(bandwidthBytesPerSecond), chunkSize);
    }

    /**
     * @param outputStream           底层输出流
     * @param bandwidthBytesPerSecond 限速（字节/秒）
     * @param chunkSize              分块大小，越小越平滑
     */
    public RateLimitedOutputStream(OutputStream outputStream,
                                   long bandwidthBytesPerSecond,
                                   int chunkSize) {
        this.outputStream = outputStream;
        this.bandwidthBytesPerSecond = bandwidthBytesPerSecond;
        this.chunkSize = Math.max(512, Math.min(chunkSize, 65536));

        // 桶容量 = 1秒流量，允许短时突发
        long capacity = bandwidthBytesPerSecond;
        this.tokenBucket = new TokenBucket(capacity, bandwidthBytesPerSecond);

        log.info("RateLimitedOutputStream created: bandwidth={}/s, chunkSize={}, capacity={}/s",
            formatBytes(bandwidthBytesPerSecond), chunkSize, formatBytes(capacity));
    }

    /**
     * 计算最佳分块大小
     * 经验公式：chunkSize = bandwidthBytesPerSecond / 50
     */
    private static int calculateOptimalChunkSize(long bandwidthBytesPerSecond) {
        if (bandwidthBytesPerSecond < 200 * 1024) {
            // 低于 200KB/s，使用 1-4KB
            return 1024;
        } else if (bandwidthBytesPerSecond < 1024 * 1024) {
            // 200KB/s - 1MB/s，使用 4-8KB
            return 4096;
        } else if (bandwidthBytesPerSecond < 5 * 1024 * 1024) {
            // 1MB/s - 5MB/s，使用 8-16KB
            return 8192;
        } else {
            // 高于 5MB/s，使用 16-32KB
            return 16384;
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    @Override
    public void write(int b) throws IOException {
        checkClosed();
        tokenBucket.acquire(1);
        outputStream.write(b);
        totalBytesWritten++;
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkClosed();
        if (len == 0) {
            return;
        }

        if (!logged) {
            log.info("RateLimitedOutputStream.write() called with len={} bytes", len);
            logged = true;
        }

        // 分块写入，使流量更平滑
        int remaining = len;
        int offset = off;

        while (remaining > 0) {
            int size = Math.min(chunkSize, remaining);
            tokenBucket.acquire(size);
            outputStream.write(b, offset, size);
            offset += size;
            remaining -= size;
            totalBytesWritten += size;
        }

        if (totalBytesWritten % (1024 * 1024) == 0) {
            double elapsed = (System.nanoTime() - startTime) / 1_000_000_000.0;
            double rate = elapsed > 0 ? (totalBytesWritten / elapsed) / 1024.0 : 0;
            log.info("Written {} bytes, actual rate: {} KB/s", totalBytesWritten, String.format("%.2f", rate));
        }
    }

    @Override
    public void flush() throws IOException {
        checkClosed();
        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            double elapsed = (System.nanoTime() - startTime) / 1_000_000_000.0;
            double rate = elapsed > 0 ? (totalBytesWritten / elapsed) / 1024.0 : 0;
            log.info("RateLimitedOutputStream closing: total bytes={}, elapsed={}s, rate={} KB/s",
                totalBytesWritten, String.format("%.2f", elapsed), String.format("%.2f", rate));
            outputStream.flush();
            outputStream.close();
        }
    }

    private void checkClosed() throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
    }

    @Override
    public boolean isReady() {
        return !closed;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        throw new UnsupportedOperationException("Async write not supported");
    }

    /**
     * 动态调整带宽
     */
    public void setBandwidth(long newBandwidth) {
        tokenBucket.setRefillRate(newBandwidth);
    }

    /**
     * 获取当前可用令牌
     */
    public long getAvailableTokens() {
        return tokenBucket.getAvailableTokens();
    }

    /**
     * 获取实际传输速率
     */
    public double getActualRate() {
        long elapsedNanos = System.nanoTime() - startTime;
        if (elapsedNanos <= 0) {
            return 0;
        }
        long elapsedSeconds = elapsedNanos / 1_000_000_000L;
        return elapsedSeconds > 0 ? (double) totalBytesWritten / elapsedSeconds : 0;
    }

    /**
     * 获取总写入字节数
     */
    public long getTotalBytesWritten() {
        return totalBytesWritten;
    }

    /**
     * 获取配置的带宽
     */
    public long getBandwidthBytesPerSecond() {
        return bandwidthBytesPerSecond;
    }

    /**
     * 获取分块大小
     */
    public int getChunkSize() {
        return chunkSize;
    }

    /**
     * 获取令牌桶利用率
     */
    public double getBucketUtilization() {
        return tokenBucket.getUtilization();
    }

    public TokenBucket getTokenBucket() {
        return tokenBucket;
    }
}
