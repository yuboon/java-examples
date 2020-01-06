package com.yuboon.springboot.gencode.configuration;

import com.yuboon.springboot.gencode.command.Command;
import com.yuboon.springboot.gencode.command.GenControllerCommand;
import com.yuboon.springboot.gencode.output.CodeOutput;
import com.yuboon.springboot.gencode.output.ConsoleOutput;
import com.yuboon.springboot.gencode.output.FileOutput;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 输出类型注册类
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/04
 */
public class CodeOutputRegister {

    private Map<String, CodeOutput> codeOutputdMap = new LinkedHashMap<String, CodeOutput>();

    public CodeOutputRegister(){
        CodeOutput consoleOutput = new ConsoleOutput();
        CodeOutput fileOutput = new FileOutput();
        registerCodeOutput(consoleOutput);
        registerCodeOutput(fileOutput);
    }

    /**
     * 注册功能
     */
    public void registerCodeOutput(CodeOutput codeOutput){
        codeOutputdMap.put(codeOutput.getOutputCode(),codeOutput);
    }

    /**
     * 根据输出码获取输出实例
     * @return
     */
    public CodeOutput getOutput(String outputCode){
        return codeOutputdMap.get(outputCode);
    }

}
