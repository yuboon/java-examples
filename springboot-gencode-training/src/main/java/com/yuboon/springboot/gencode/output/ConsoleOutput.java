package com.yuboon.springboot.gencode.output;


import cn.hutool.core.lang.Console;
import com.yuboon.springboot.gencode.metadata.Table;
import com.yuboon.springboot.gencode.template.CodeTemplate;

/**
 * 此处为类介绍
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/08
 */
public class ConsoleOutput extends CodeOutput {

    public ConsoleOutput(){
        this.code = "01";
        this.name = "Console";
    }

    @Override
    public void out(Table table, String content, CodeTemplate template){
        Console.log(content);
    };

}
