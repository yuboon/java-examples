package com.example.asn1.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * ASN.1解析响应DTO
 *
 * 
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Asn1ParseResponse {

    /**
     * 解析是否成功
     */
    private boolean success;

    /**
     * 响应消息
     */
    private String message;

    /**
     * ASN.1结构树
     */
    private Asn1Structure rootStructure;

    /**
     * 警告信息列表
     */
    private List<String> warnings;

    /**
     * 元数据信息
     */
    private Map<String, Object> metadata;

    /**
     * ASN.1结构数据类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Asn1Structure {
        /**
         * 标签名称
         */
        private String tag;

        /**
         * 标签编号
         */
        private int tagNumber;

        /**
         * 标签类别
         */
        private String tagClass;

        /**
         * 数据类型
         */
        private String type;

        /**
         * 数据值
         */
        private String value;

        /**
         * 数据长度
         */
        private int length;

        /**
         * 偏移量
         */
        private int offset;

        /**
         * 子结构列表
         */
        private List<Asn1Structure> children;

        /**
         * 属性信息
         */
        private Map<String, Object> properties;
    }
}