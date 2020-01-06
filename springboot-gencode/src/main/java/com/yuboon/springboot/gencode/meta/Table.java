package com.yuboon.springboot.gencode.meta;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;

/**
 * 表元数据
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/04
 */
public class Table {

    // 编码
    private String code;

    // 类名
    private String className;

    // 备注
    private String comment;

    // 创建时间
    private String createDate = DateUtil.today();

    // 字段列
    private List<TableColumn> columnList;

    public String getClassName() {
        this.className = StrUtil.upperFirst(StrUtil.toCamelCase(code));
        return className;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<TableColumn> getColumnList() {
        return columnList;
    }

    public void setColumnList(List<TableColumn> columnList) {
        this.columnList = columnList;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }
}
