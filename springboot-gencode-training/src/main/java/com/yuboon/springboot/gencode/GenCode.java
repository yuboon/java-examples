package com.yuboon.springboot.gencode;

import cn.hutool.core.lang.Console;
import cn.hutool.crypto.symmetric.DES;
import cn.hutool.json.JSONUtil;
import com.yuboon.springboot.gencode.command.Command;
import com.yuboon.springboot.gencode.configuration.GenCodeConfiguration;
import com.yuboon.springboot.gencode.configuration.TypeMapping;
import com.yuboon.springboot.gencode.metadata.MetaData;
import com.yuboon.springboot.gencode.metadata.Table;
import com.yuboon.springboot.gencode.output.CodeOutput;
import com.yuboon.springboot.gencode.output.ConsoleOutput;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.dialect.SpringStandardDialect;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import javax.swing.*;

/**
 * 此处为类介绍
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/08
 */
public class GenCode {

    private SpringTemplateEngine springTemplateEngine;

    private GenCodeConfiguration genCodeConfiguration;

    public GenCode(GenCodeConfiguration configuration) {
        this.initTemplateEngine();
        this.genCodeConfiguration = configuration;
    }

    public void initTemplateEngine(){
        springTemplateEngine = new SpringTemplateEngine();
        IDialect dialect = new SpringStandardDialect();
        springTemplateEngine.setDialect(dialect);
        // 文本解析器
        StringTemplateResolver resolverText = new StringTemplateResolver();
        resolverText.setCacheable(true);
        resolverText.setTemplateMode(TemplateMode.TEXT);
        // 添加解析器
        springTemplateEngine.addTemplateResolver(resolverText);
    }

    public void genCode(String tableName,Command command,CodeOutput codeOutput){
        // 取元数据
        Table table = MetaData.getTableInfo(tableName,genCodeConfiguration.getTypeMapping());
        Console.log("table = {}", JSONUtil.toJsonStr(table));
        command.execute(table,springTemplateEngine,codeOutput);
    }

}
