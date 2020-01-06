package com.yuboon.springboot.gencode.output;

import com.yuboon.springboot.gencode.meta.Table;
import com.yuboon.springboot.gencode.template.CodeTemplate;

/**
 * 输出接口
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/04
 */
public abstract class CodeOutput {

    protected String outputCode;

    /**
     * 输出文件
     * @param table
     * @param content
     * @param template
     */
    public abstract void out(Table table, String content, CodeTemplate template);

    public String getOutputCode() {
        return outputCode;
    }

}
