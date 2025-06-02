package com.yuboon.springboot.gencode.output;


import cn.hutool.core.lang.Console;
import com.yuboon.springboot.gencode.metadata.Table;
import com.yuboon.springboot.gencode.template.CodeTemplate;

/**
 * 此处为类介绍
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/08
 */
public abstract class CodeOutput {

    protected String code;

    protected String name;

    public abstract void out(Table table, String content, CodeTemplate template);

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
