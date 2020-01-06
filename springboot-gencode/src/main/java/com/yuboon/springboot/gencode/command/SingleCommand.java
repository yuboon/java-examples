package com.yuboon.springboot.gencode.command;

import cn.hutool.core.bean.BeanUtil;
import com.yuboon.springboot.gencode.meta.Table;
import com.yuboon.springboot.gencode.output.CodeOutput;
import com.yuboon.springboot.gencode.template.CodeTemplate;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import java.util.Map;

/**
 * 命令接口
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/04
 */
public abstract class SingleCommand extends Command {

    // 命令对应的模板
    protected CodeTemplate template;

    // 执行命令
    @Override
    public void execute(Table table, SpringTemplateEngine springTemplateEngine, CodeOutput codeOutput){
        Map map = BeanUtil.beanToMap(table);
        this.process(table,map);
        Context context = new Context();
        context.setVariables(map);
        String result = springTemplateEngine.process(template.read(),context);
        codeOutput.out(table,result,template);
    }

    // 使用者可添加自定义处理过程
    public abstract void process(Table table,Map map);

}
