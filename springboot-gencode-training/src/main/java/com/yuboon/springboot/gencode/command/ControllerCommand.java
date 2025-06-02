package com.yuboon.springboot.gencode.command;

import cn.hutool.core.bean.BeanUtil;
import com.yuboon.springboot.gencode.metadata.Table;
import com.yuboon.springboot.gencode.output.CodeOutput;
import com.yuboon.springboot.gencode.template.CodeTemplate;
import com.yuboon.springboot.gencode.template.ControllerCodeTemplate;
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
public class ControllerCommand extends SingleCommand {

    public ControllerCommand(){
        this.code = "01";
        this.name = "生成Controller";
        this.codeTemplate = new ControllerCodeTemplate();
    }

    @Override
    public void process(Table table, Map<String, Object> map) {

    }
}
