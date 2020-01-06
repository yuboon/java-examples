package com.yuboon.springboot.gencode;

import com.yuboon.springboot.gencode.command.Command;
import com.yuboon.springboot.gencode.configuration.GenCodeConfiguration;
import com.yuboon.springboot.gencode.meta.MetaData;
import com.yuboon.springboot.gencode.meta.Table;
import com.yuboon.springboot.gencode.output.CodeOutput;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.dialect.SpringStandardDialect;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * 代码生成主类
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/04
 */
public class GenCode {

    // 全局配置
    private GenCodeConfiguration configuration;

    // 模板引擎
    protected SpringTemplateEngine springTemplateEngine;

    public GenCode(GenCodeConfiguration configuration){
        this.configuration = configuration;
        this.initTemplateEngine();
    }

    /**
     * 初始化模板
     */
    private void initTemplateEngine(){
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

    /**
     * 命令处理
     * @param tableName
     * @param command
     * @param codeOutput
     */
    public void process(String tableName,Command command,CodeOutput codeOutput){
        Table tableInfo = MetaData.getTableInfo(tableName,configuration.getTypeMapping());
        command.execute(tableInfo,springTemplateEngine,codeOutput);
    }

}
