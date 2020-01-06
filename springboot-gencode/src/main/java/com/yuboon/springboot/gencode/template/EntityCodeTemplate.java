package com.yuboon.springboot.gencode.template;

/**
 * 控制类模板
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/04
 */
public class EntityCodeTemplate extends CodeTemplate {

    public EntityCodeTemplate() {
        this.tplName = "控制器模板";
        this.tplPath = "templates/Entity.tpl";
        this.fileTag = "";
        this.fileSuffix = ".java";
    }

}
