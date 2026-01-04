package com.example.pipeline.nodes;

import com.example.pipeline.model.OrderRequest;
import com.example.pipeline.pipeline.PipelineContext;
import com.example.pipeline.pipeline.PipelineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

/**
 * 参数校验节点
 * 使用 JSR-303 验证请求参数
 */
@Component
public class ParamValidateNode extends AbstractOrderNode {

    private static final Logger logger = LoggerFactory.getLogger(ParamValidateNode.class);

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Override
    public void execute(PipelineContext<OrderRequest> context) throws PipelineException {
        OrderRequest request = getRequest(context);

        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(request);

        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder("参数校验失败: ");
            for (ConstraintViolation<OrderRequest> violation : violations) {
                sb.append(violation.getMessage()).append("; ");
            }
            throw new PipelineException(getName(), sb.toString());
        }

        // 额外业务校验
        if (request.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new PipelineException(getName(), "订单总金额必须大于0");
        }

        logger.info("参数校验通过: userId={}, productId={}, amount={}",
                request.getUserId(), request.getProductId(), request.getTotalAmount());
    }
}
