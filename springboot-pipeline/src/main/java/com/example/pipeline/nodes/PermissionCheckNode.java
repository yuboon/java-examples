package com.example.pipeline.nodes;

import com.example.pipeline.model.OrderRequest;
import com.example.pipeline.pipeline.PipelineContext;
import com.example.pipeline.pipeline.PipelineException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 权限校验节点
 * 检查用户是否有创建订单的权限
 */
@Slf4j
@Component
public class PermissionCheckNode extends AbstractOrderNode {

    // 模拟黑名单用户
    private static final Set<Long> BLACKLIST_USERS = Set.of(999L, 888L);

    // 模拟被封禁的用户
    private static final Set<Long> BANNED_USERS = new HashSet<>();

    static {
        BANNED_USERS.add(666L);
    }

    @Override
    public void execute(PipelineContext<OrderRequest> context) throws PipelineException {
        OrderRequest request = getRequest(context);
        Long userId = request.getUserId();

        // 检查黑名单
        if (BLACKLIST_USERS.contains(userId)) {
            throw new PipelineException(getName(), "用户在黑名单中，无法创建订单");
        }

        // 检查封禁状态
        if (BANNED_USERS.contains(userId)) {
            throw new PipelineException(getName(), "用户已被封禁，无法创建订单");
        }

        log.info("权限校验通过: userId={}", userId);
    }
}
