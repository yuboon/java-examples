package com.yuboon.springboot.gencode.template;

/**
 * VO模板
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/04
 */
public class VoCodeTemplate extends CodeTemplate {

    public VoCodeTemplate() {
        this.tplName = "VO模板";
        this.tplPath = "templates/Vo.tpl";
        this.fileTag = "Vo";
        this.fileSuffix = ".java";
    }

}
