package com.yuboon.springboot.gencode;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.yuboon.springboot.gencode.command.Command;
import com.yuboon.springboot.gencode.configuration.GenCodeConfiguration;
import com.yuboon.springboot.gencode.output.CodeOutput;

import java.util.List;
import java.util.Scanner;

/**
 * 交互控制
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/04
 */
public class GenCodeRunner {

    public void run(){
        Scanner scanner = new Scanner(System.in);
        scanner.useDelimiter("\n");

        // 初始化代码生成配置
        GenCodeConfiguration genCodeConfiguration = new GenCodeConfiguration();
        GenCode genCode = new GenCode(genCodeConfiguration);

        // 控制用户交互过程
        Command command = this.selectCommand(genCodeConfiguration,scanner);
        CodeOutput output = this.selectOutput(genCodeConfiguration,scanner);
        String tableName = this.selectTableName(scanner);

        // 生成
        Console.log("\n");
        Console.log("开始生成....");
        genCode.process(tableName,command,output);
        Console.log("生成完成....");

    }

    private Command selectCommand(GenCodeConfiguration genCodeConfiguration, Scanner scanner){
        Console.log("\n第1步：请从下方清单选择要生成的代码");
        Console.log("---------------------");

//        Console.log("01-Controller");
//        Console.log("02-Entity");
//        Console.log("03-Controller & Entity代码");
//        Console.log("04-Vo");

        List<Command> commands = CollectionUtil.newArrayList(genCodeConfiguration.getCommands());
        for(Command command : commands){
            Console.log("{}-{}",command.getCommandCode(),command.getCommandName());
        }

        Console.log("---------------------");
        Console.log("请输入选择：");
        Command command = null;
        while(scanner.hasNext()){
            String commandCode = scanner.next();
            command = genCodeConfiguration.getCommand(commandCode);
            if(command == null){
                Console.error("根据功能码{}未找到生成命令，请检查!!!",commandCode);
                break;
            }
            Console.log("你选择的功能码：" + commandCode);
            break;
        }
        return command;
    }

    private CodeOutput selectOutput(GenCodeConfiguration genCodeConfiguration,Scanner scanner){
        CodeOutput codeOutput = null;
        Console.log("\n第2步：请从下方清单选择要输出的代码方式");
        Console.log("---------------------");
        Console.log("01-Console");
        Console.log("02-File");
        Console.log("---------------------");
        Console.log("请输入选择：");
        while(scanner.hasNext()){
            String inputCode = scanner.next();
            codeOutput = genCodeConfiguration.getOutput(inputCode);
            if(codeOutput == null){
                Console.error("根据功能码{}未找到输出类型，请检查!!!",inputCode);
                continue;
            }
            Console.log("你选择的功能码：" + inputCode);
            break;
        }
        return codeOutput;
    }

    private String selectTableName(Scanner scanner){
        String inputTableName = null;
        Console.log("\n第3步：请输入要生成代码对应的表名称：");
        while(scanner.hasNext()){
            inputTableName = scanner.next();
            if(StrUtil.isBlank(inputTableName)){
                Console.error("表名称不能为空!!!");
                continue;
            }
            Console.log("你输入的表名：" + inputTableName);
            break;
        }
        return inputTableName;
    }

    public static void main(String[] args) {
        new GenCodeRunner().run();
    }

}
