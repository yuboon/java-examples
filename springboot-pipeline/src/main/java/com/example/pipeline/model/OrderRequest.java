package com.example.pipeline.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单创建请求
 */
@Data
public class OrderRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    @JsonProperty("user_id")
    private Long userId;

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    @JsonProperty("product_id")
    private Long productId;

    /**
     * 商品名称
     */
    @NotBlank(message = "商品名称不能为空")
    @JsonProperty("product_name")
    private String productName;

    /**
     * 数量
     */
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    @JsonProperty("quantity")
    private Integer quantity;

    /**
     * 单价
     */
    @NotNull(message = "单价不能为空")
    @JsonProperty("unit_price")
    private BigDecimal unitPrice;

    /**
     * 收货地址
     */
    @NotBlank(message = "收货地址不能为空")
    @JsonProperty("address")
    private String address;

    /**
     * 备注
     */
    @JsonProperty("remark")
    private String remark;

    /**
     * 订单来源
     */
    @JsonProperty("source")
    private String source = "WEB";

    /**
     * 是否跳过风控（用于测试）
     */
    @JsonProperty("skip_risk_check")
    private Boolean skipRiskCheck = false;

    /**
     * 计算总金额
     */
    public BigDecimal getTotalAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
