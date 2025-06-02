package com.yuboon.springboot.gencode.command;

import cn.hutool.core.bean.BeanUtil;
import com.yuboon.springboot.gencode.metadata.Table;
import com.yuboon.springboot.gencode.output.CodeOutput;
import com.yuboon.springboot.gencode.template.CodeTemplate;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import java.util.Map;

/**
 * 此处为类介绍
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/08
 */
public abstract class Command {

    protected String code;

    protected String name;

    /**
     * 执行
     */
    public abstract void execute(Table table, SpringTemplateEngine springTemplateEngine, CodeOutput codeOutput);

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
