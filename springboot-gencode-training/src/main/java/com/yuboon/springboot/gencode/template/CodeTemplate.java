package com.yuboon.springboot.gencode.template;

import cn.hutool.core.io.IoUtil;
import com.sun.org.apache.bcel.internal.classfile.Code;

import java.io.InputStream;

/**
 * 此处为类介绍
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/08
 */
public abstract class CodeTemplate {

    protected String tplPath;

    protected String tplName;

    // controller service
    protected String fileTag;

    // 文件后缀
    protected String fileSuffix;

    public String read(){
        InputStream is = this.getClass().getResourceAsStream(tplPath);
        String result = IoUtil.read(is,"UTF-8");
        IoUtil.close(is);
        return result;
    }

    public String getTplPath() {
        return tplPath;
    }

    public void setTplPath(String tplPath) {
        this.tplPath = tplPath;
    }

    public String getTplName() {
        return tplName;
    }

    public void setTplName(String tplName) {
        this.tplName = tplName;
    }

    public String getFileTag() {
        return fileTag;
    }

    public void setFileTag(String fileTag) {
        this.fileTag = fileTag;
    }

    public String getFileSuffix() {
        return fileSuffix;
    }

    public void setFileSuffix(String fileSuffix) {
        this.fileSuffix = fileSuffix;
    }
}
