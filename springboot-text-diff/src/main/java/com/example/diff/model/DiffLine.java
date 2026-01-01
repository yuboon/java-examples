package com.example.diff.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiffLine {
    /**
     * 行类型: EQUAL（相同）, INSERT（新增）, DELETE（删除）, CHANGE（修改）
     */
    private String type;

    /**
     * 原始行的内容
     */
    private String originalLine;

    /**
     * 修改后行的内容
     */
    private String revisedLine;
}
