package com.example.memviz.util;

import java.io.IOException;
import java.nio.file.*;

public class SafeExecs {
    public static void assertDiskHasSpace(Path dir, long minFreeBytes) throws IOException {
        FileStore store = Files.getFileStore(dir);
        if (store.getUsableSpace() < minFreeBytes) {
            throw new IllegalStateException("Low disk space for heap dump: need " + minFreeBytes + " bytes free");
        }
    }
}