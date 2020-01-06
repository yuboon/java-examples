package com.yuboon.springboot.gencode.output;


import cn.hutool.core.lang.Console;
import com.yuboon.springboot.gencode.meta.Table;
import com.yuboon.springboot.gencode.template.CodeTemplate;

/**
 * 控制台输出
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/04
 */
public class ConsoleOutput extends CodeOutput {

    public ConsoleOutput() {
        this.outputCode = "01";
    }

    @Override
    public void out(Table table, String content, CodeTemplate template) {
        Console.log("\n");
        Console.log("-----------------------------");
        Console.log(content);
        Console.log("-----------------------------");
    }
}
