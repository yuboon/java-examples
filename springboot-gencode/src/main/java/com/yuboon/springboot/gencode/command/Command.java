package com.yuboon.springboot.gencode.command;

import com.yuboon.springboot.gencode.meta.Table;
import com.yuboon.springboot.gencode.output.CodeOutput;
import org.thymeleaf.spring5.SpringTemplateEngine;

/**
 * 命令接口
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/04
 */
public abstract class Command {

    // 命令名称
    protected String commandName;

    // 命名编码对应用户使用时的功能码
    protected String commandCode;

    /**
     * 执行命令
     * @param tableInfo
     * @param springTemplateEngine
     * @param codeOutput
     */
    public abstract void execute(Table tableInfo,SpringTemplateEngine springTemplateEngine,CodeOutput codeOutput);

    public String getCommandCode() {
        return commandCode;
    }

    public String getCommandName() {
        return commandName;
    }

}
