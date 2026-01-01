package com.example.diff.model;

import lombok.Data;
import java.util.List;

@Data
public class DiffChange {
    private String type;        // INSERT, DELETE, CHANGE
    private int sourceLine;     // 原配置行号
    private int targetLine;     // 新配置行号
    private List<String> originalLines;
    private List<String> revisedLines;
}
