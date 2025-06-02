package com.yuboon.springboot.gencode.output;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import com.yuboon.springboot.gencode.metadata.Table;
import com.yuboon.springboot.gencode.template.CodeTemplate;

import java.io.File;

/**
 * 此处为类介绍
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/08
 */
public class FiletOutput extends CodeOutput {

    public FiletOutput(){
        this.code = "02";
        this.name = "File";
    }

    @Override
    public void out(Table table, String content, CodeTemplate template){
        String fileName = "dist/" + table.getClassName() + template.getFileTag() + template.getFileSuffix();
        File file = new File(fileName);
        FileUtil.writeBytes(content.getBytes(),file);
        Console.log("文件位置:" + file.getAbsolutePath());
    };

}
