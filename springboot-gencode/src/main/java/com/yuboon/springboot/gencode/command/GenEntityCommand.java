package com.yuboon.springboot.gencode.command;

import com.yuboon.springboot.gencode.meta.Table;
import com.yuboon.springboot.gencode.template.ControllerCodeTemplate;
import com.yuboon.springboot.gencode.template.EntityCodeTemplate;

import java.util.Map;

/**
 * 控制器生成命令
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/04
 */
public class GenEntityCommand extends SingleCommand {

    public GenEntityCommand() {
        this.commandName = "Entity生成命令";
        this.commandCode = "02";
        this.template = new EntityCodeTemplate();
    }

    @Override
    public void process(Table table, Map map) {
       // 留给用户自定义处理过程，比如一些自定义参数的处理加一些控制等等，此处暂不需要
    }
}
