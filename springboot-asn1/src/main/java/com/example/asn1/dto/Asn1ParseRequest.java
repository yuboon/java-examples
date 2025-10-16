package com.example.asn1.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * ASN.1解析请求DTO
 *
 * 
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Asn1ParseRequest {

    /**
     * ASN.1数据，不能为空
     */
    @NotBlank(message = "ASN.1数据不能为空")
    private String asn1Data;

    /**
     * 编码类型，默认为HEX
     */
    private String encodingType = "HEX";

    /**
     * 是否输出详细信息
     */
    private boolean verbose = false;
}