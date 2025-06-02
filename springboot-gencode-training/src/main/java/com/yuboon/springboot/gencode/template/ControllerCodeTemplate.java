package com.yuboon.springboot.gencode.template;

import cn.hutool.core.io.IoUtil;

import java.io.InputStream;

/**
 * 此处为类介绍
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/08
 */
public class ControllerCodeTemplate extends CodeTemplate {

    public ControllerCodeTemplate(){
        this.tplPath = "/template/Controller.tpl";
        this.fileTag = "Controller";
        this.fileSuffix = ".java";
    }

}
