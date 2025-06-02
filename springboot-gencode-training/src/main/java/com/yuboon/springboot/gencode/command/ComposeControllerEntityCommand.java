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
public class ComposeControllerEntityCommand extends ComposeCommand {

    protected List<SingleCommand> singleCommandList = CollectionUtil.newArrayList();

    public ComposeControllerEntityCommand(){
        this.code = "03";
        this.name = "生成Controller & Entity";
        this.singleCommandList.add(new ControllerCommand());
        this.singleCommandList.add(new EntityCommand());
    }

}
