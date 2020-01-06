package com.yuboon.springboot.gencode.template;

import cn.hutool.core.io.IoUtil;

import java.io.InputStream;

/**
 * 代码模板类
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/04
 */
public abstract class CodeTemplate {

    protected String tplName;

    protected String tplPath;

    protected String fileSuffix;

    protected String fileTag;

    /**
     * 读取模板文件内容
     * @return
     */
    public String read(){
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(this.tplPath);
        String tpl = IoUtil.read(is, "UTF-8");
        IoUtil.close(is);
        return tpl;
    }


    public String getTplName() {
        return tplName;
    }

    public void setTplName(String tplName) {
        this.tplName = tplName;
    }

    public String getTplPath() {
        return tplPath;
    }

    public void setTplPath(String tplPath) {
        this.tplPath = tplPath;
    }

    public String getFileSuffix() {
        return fileSuffix;
    }

    public void setFileSuffix(String fileSuffix) {
        this.fileSuffix = fileSuffix;
    }

    public String getFileTag() {
        return fileTag;
    }

    public void setFileTag(String fileTag) {
        this.fileTag = fileTag;
    }
}
