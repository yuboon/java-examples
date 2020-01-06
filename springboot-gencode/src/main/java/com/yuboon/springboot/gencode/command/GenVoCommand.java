package com.yuboon.springboot.gencode.command;

import com.yuboon.springboot.gencode.meta.Table;
import com.yuboon.springboot.gencode.template.EntityCodeTemplate;
import com.yuboon.springboot.gencode.template.VoCodeTemplate;

import java.util.Map;

/**
 * 控制器生成命令
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/04
 */
public class GenVoCommand extends SingleCommand {

    public GenVoCommand() {
        this.commandName = "Vo生成命令";
        this.commandCode = "04";
        this.template = new VoCodeTemplate();
    }

    @Override
    public void process(Table table, Map map) {

    }
}
