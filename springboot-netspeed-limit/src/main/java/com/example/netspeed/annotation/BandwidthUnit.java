package com.example.netspeed.annotation;

/**
 * 带宽单位枚举
 */
public enum BandwidthUnit {
    B(1),
    KB(1024),
    MB(1024 * 1024),
    GB(1024 * 1024 * 1024);

    private final long bytesPerSecond;

    BandwidthUnit(long bytesPerSecond) {
        this.bytesPerSecond = bytesPerSecond;
    }

    public long toBytesPerSecond(long value) {
        return value * bytesPerSecond;
    }

    public long getBytesPerUnit() {
        return bytesPerSecond;
    }

    public static String formatBytes(long bytes) {
        if (bytes < KB.getBytesPerUnit()) {
            return bytes + " B";
        } else if (bytes < MB.getBytesPerUnit()) {
            return String.format("%.2f KB", bytes / (double) KB.getBytesPerUnit());
        } else if (bytes < GB.getBytesPerUnit()) {
            return String.format("%.2f MB", bytes / (double) MB.getBytesPerUnit());
        } else {
            return String.format("%.2f GB", bytes / (double) GB.getBytesPerUnit());
        }
    }
}
