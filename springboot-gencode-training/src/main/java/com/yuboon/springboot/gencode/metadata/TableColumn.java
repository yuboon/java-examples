package com.yuboon.springboot.gencode.metadata;

import cn.hutool.core.util.StrUtil;

/**
 * 此处为类介绍
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/08
 */
public class TableColumn {

    private String code;

    private String type;

    private String comment;

    private String attrName;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getAttrName() {
        this.attrName = StrUtil.toCamelCase(code);
        return attrName;
    }

    public void setAttrName(String attrName) {
        this.attrName = attrName;
    }
}
