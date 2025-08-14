package com.example.memviz.service;

import com.example.memviz.util.SafeExecs;
import org.springframework.stereotype.Service;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class HeapDumpService {

    private static final String HOTSPOT_BEAN = "com.sun.management:type=HotSpotDiagnostic";
    private static final String DUMP_METHOD = "dumpHeap";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * 生成 HPROF 快照文件
     *
     * @param live 是否仅包含存活对象（会触发一次 STW）
     * @param dir  目录（建议挂到独立磁盘/大空间）
     * @return hprof 文件路径
     */
    public File dump(boolean live, File dir) throws Exception {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Cannot create dump dir: " + dir);
        }
        String name = "heap_" + LocalDateTime.now().format(FMT) + (live ? "_live" : "") + ".hprof";
        File out = new File(dir, name);
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ObjectName objName = new ObjectName(HOTSPOT_BEAN);
        // 防御：限制最大文件大小（环境变量控制）
        SafeExecs.assertDiskHasSpace(dir.toPath(), 512L * 1024 * 1024); // 至少 512MB 空间

        server.invoke(objName, DUMP_METHOD, new Object[]{out.getAbsolutePath(), live},
                new String[]{"java.lang.String", "boolean"});
        return out;
    }
}