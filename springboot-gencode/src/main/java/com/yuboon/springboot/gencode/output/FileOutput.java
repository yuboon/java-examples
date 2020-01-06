package com.yuboon.springboot.gencode.output;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Console;
import com.yuboon.springboot.gencode.meta.Table;
import com.yuboon.springboot.gencode.template.CodeTemplate;

import java.io.File;
import java.io.IOException;

/**
 * 文件输出
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/04
 */
public class FileOutput extends CodeOutput {

    public FileOutput() {
        this.outputCode = "02";
    }

    @Override
    public void out(Table table, String content, CodeTemplate template) {
        String fileName = "dist/" + table.getClassName() + template.getFileTag() + template.getFileSuffix();
        File file = new File(fileName);
        FileUtil.writeBytes(content.getBytes(),file);
        Console.log("文件位置:" + file.getAbsolutePath());
    }

}
