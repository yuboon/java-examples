package com.yuboon.springboot.gencode.configuration;

import com.yuboon.springboot.gencode.command.Command;
import com.yuboon.springboot.gencode.command.ComposeControllerEntityCommand;
import com.yuboon.springboot.gencode.command.ControllerCommand;
import com.yuboon.springboot.gencode.command.EntityCommand;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 命令注册类
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/04
 */
public class CommandRegister {

    private Map<String, Command> commandMap = new LinkedHashMap<String, Command>();

    public CommandRegister(){
        // 默认注册的命令
        registerCommand(new ControllerCommand());
        registerCommand(new EntityCommand());
    }

    /**
     * 注册功能
     */
    public void registerCommand(Command command){
        commandMap.put(command.getCode(),command);
    }

    /**
     * 根据命令码获取命令
     * @param commandCode
     * @return
     */
    public Command getCommand(String commandCode){
        return commandMap.get(commandCode);
    }

    /**
     * 根据命令码获取命令
     * @param commandClass
     * @return
     */
    public Command getCommand(Class<? extends Command> commandClass){
        Iterator<Command> iterator = commandMap.values().iterator();
        while(iterator.hasNext()){
            Command command = iterator.next();
            if(command.getClass()  ==  commandClass){
                return command;
            }
        }
        return null;
    }

    /**
     * 根据命令码获取命令集合
     * @return
     */
    public Collection<Command> getCommands(){
        return commandMap.values();
    }

}
