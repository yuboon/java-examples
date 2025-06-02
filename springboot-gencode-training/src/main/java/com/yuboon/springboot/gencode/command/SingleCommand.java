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
public abstract class SingleCommand extends Command {

    protected CodeTemplate codeTemplate;

    public SingleCommand(){
        this.code = "01";
        this.name = "生成Controller";
        this.codeTemplate = new ControllerCodeTemplate();
    }

    /**
     * 执行
     */
    @Override
    public void execute(Table table, SpringTemplateEngine springTemplateEngine, CodeOutput codeOutput){
        Context context = new Context();
        Map<String,Object> map = BeanUtil.beanToMap(table);
        context.setVariables(map);
        this.process(table,map);
        String result = springTemplateEngine.process(codeTemplate.read(),context);
        codeOutput.out(table,result,codeTemplate);
    };

    public abstract void process(Table table,Map<String,Object> map);
}
