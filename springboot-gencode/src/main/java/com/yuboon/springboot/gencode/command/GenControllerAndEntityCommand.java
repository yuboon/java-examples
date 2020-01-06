package com.yuboon.springboot.gencode.command;

/**
 * 控制器与实体组合生成命令
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/04
 */
public class GenControllerAndEntityCommand extends ComposeCommand {

    public GenControllerAndEntityCommand() {
        this.commandName = "Controller & Entity 生成命令";
        this.commandCode = "03";
        singleCommands.add(new GenControllerCommand());
        singleCommands.add(new GenEntityCommand());
    }

}
