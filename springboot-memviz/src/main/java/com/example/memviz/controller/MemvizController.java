package com.example.memviz.controller;

import com.example.memviz.model.GraphModel;
import com.example.memviz.service.HeapDumpService;
import com.example.memviz.service.HprofParseService;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Predicate;

@RestController
@RequestMapping("/memviz")
public class MemvizController {

    private final HeapDumpService dumpService;
    private final HprofParseService parseService;

    public MemvizController(HeapDumpService dumpService, HprofParseService parseService) {
        this.dumpService = dumpService;
        this.parseService = parseService;
    }

    /**
     * 触发一次快照，返回文件名（安全：默认 live=false）
     */
    @PostMapping("/snapshot")
    public Map<String, String> snapshot(@RequestParam(defaultValue = "false") boolean live,
                                        @RequestParam(defaultValue = "/tmp/memviz") String dir) throws Exception {
        File f = dumpService.dump(live, new File(dir));
        return Map.of("file", f.getAbsolutePath());
    }

    /**
     * 解析指定快照 → 图模型（支持过滤&折叠）
     */
    @GetMapping(value = "/graph", produces = MediaType.APPLICATION_JSON_VALUE)
    public GraphModel graph(@RequestParam String file,
                            @RequestParam(required = false) String include, // 例如: com.myapp.,java.util.HashMap
                            @RequestParam(defaultValue = "true") boolean collapseCollections) throws Exception {

        Predicate<String> filter = null;
        if (StringUtils.hasText(include)) {
            String[] prefixes = include.split(",");
            filter = fqcn -> {
                // 总是包含重要的基础类，以便显示大对象
                if (fqcn.equals("java.lang.String") || fqcn.equals("byte[]") || 
                    fqcn.startsWith("java.lang.String[") || fqcn.startsWith("java.util.ArrayList")) {
                    return true;
                }
                // 检查用户指定的前缀
                for (String p : prefixes) if (fqcn.startsWith(p.trim())) return true;
                return false;
            };
        }
        return parseService.parseToGraph(new File(file), filter, collapseCollections);
    }
}