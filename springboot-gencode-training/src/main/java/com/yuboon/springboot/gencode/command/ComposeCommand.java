package com.yuboon.springboot.gencode.command;

import cn.hutool.core.collection.CollectionUtil;
import com.yuboon.springboot.gencode.metadata.Table;
import com.yuboon.springboot.gencode.output.CodeOutput;
import com.yuboon.springboot.gencode.template.ControllerCodeTemplate;
import org.thymeleaf.spring5.SpringTemplateEngine;

import java.util.List;
import java.util.Map;

/**
 * 此处为类介绍
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/08
 */
public abstract class ComposeCommand extends Command {

    protected List<SingleCommand> singleCommandList = CollectionUtil.newArrayList();

    @Override
    public void execute(Table table, SpringTemplateEngine springTemplateEngine, CodeOutput codeOutput) {
        for (SingleCommand singleCommand : singleCommandList){
            singleCommand.execute(table,springTemplateEngine,codeOutput);
        }
    }

}
