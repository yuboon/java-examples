package com.yuboon.springboot.gencode.command;

import com.yuboon.springboot.gencode.metadata.Table;
import com.yuboon.springboot.gencode.template.CodeTemplate;
import com.yuboon.springboot.gencode.template.EntityCodeTemplate;

import java.util.Map;

/**
 * 此处为类介绍
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/08
 */
public class EntityCommand extends SingleCommand {

    public EntityCommand(){
        this.code = "02";
        this.name = "生成Entity";
        this.codeTemplate = new EntityCodeTemplate();
    }

    @Override
    public void process(Table table, Map<String, Object> map) {

    }
}
