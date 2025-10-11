package com.example.objectversion.aspect;

import com.example.objectversion.annotation.Audit;
import com.example.objectversion.model.AuditLog;
import com.example.objectversion.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.javers.core.Javers;
import org.javers.core.diff.Diff;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 审计切面，自包含的审计逻辑，无需额外服务类
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final Javers javers;

    // 内存存储审计日志（生成环境需要存放到数据库中）
    private final List<AuditLog> auditTimeline = new CopyOnWriteArrayList<>();
    private final Map<String, List<AuditLog>> auditByEntity = new ConcurrentHashMap<>();
    private final AtomicLong auditSequence = new AtomicLong(0);

    // 数据存储，用于快照对比
    private final Map<String, Object> dataStore = new ConcurrentHashMap<>();

    @Around("@annotation(auditAnnotation)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audit auditAnnotation) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        // 提取实体ID
        String entityId = extractEntityId(null, args, paramNames, auditAnnotation);
        log.info("审计方法: {}, 提取到ID: {}", method.getName(), entityId);
        if (entityId == null) {
            log.error("无法提取实体ID，跳过审计: {}, 参数名: {}", method.getName(), paramNames != null ? String.join(",", paramNames) : "null");
            return joinPoint.proceed();
        }

        // 提取实体对象（DELETE操作可能不需要）
        Object entity = null;
        if (auditAnnotation.entityIndex() >= 0 && auditAnnotation.entityIndex() < args.length) {
            entity = args[auditAnnotation.entityIndex()];
        }

        // 提取操作人
        String actor = extractActor(args, paramNames, auditAnnotation);

        // 获取操作类型
        Audit.ActionType actionType = determineActionType(auditAnnotation, method.getName());

        // 执行前快照
        Object beforeSnapshot = null;
        if (actionType == Audit.ActionType.DELETE) {
            // 删除操作：需要知道要删除什么，从数据存储中获取当前实体
            beforeSnapshot = dataStore.get(buildKey(Product.class, entityId)); // 假设是Product，实际应该动态确定
        } else if (entity != null) {
            // 其他操作：从存储中获取历史快照
            beforeSnapshot = dataStore.get(buildKey(entity.getClass(), entityId));
        }

        // 执行原方法
        Object result = joinPoint.proceed();

        // 执行后快照 - 这里需要通过业务逻辑获取最新状态
        // 简化处理：对于CREATE和DELETE，直接使用null或新对象
        // 对于UPDATE，这里应该重新查询，但为了演示简化处理
        Object afterSnapshot = determineAfterSnapshot(entity, actionType, entityId);

        // 比较差异并记录审计日志
        Diff diff = javers.compare(beforeSnapshot, afterSnapshot);

        // 确定实体类型
        String entityType = entity != null ? entity.getClass().getSimpleName() : "Product";

        // DELETE操作或有变更时记录审计
        if (diff.hasChanges() || beforeSnapshot == null || actionType == Audit.ActionType.DELETE) {
            recordAudit(
                entityType,
                entityId,
                actionType.name(),
                actor,
                javers.getJsonConverter().toJson(diff)
            );
        }

        // 更新数据存储
        if (actionType != Audit.ActionType.DELETE) {
            Class<?> entityClass = entity != null ? entity.getClass() : Product.class;
            dataStore.put(buildKey(entityClass, entityId), afterSnapshot);
        } else {
            Class<?> entityClass = entity != null ? entity.getClass() : Product.class;
            dataStore.remove(buildKey(entityClass, entityId));
        }

        return result;
    }

    /**
     * 提取操作人
     */
    private String extractActor(Object[] args, String[] paramNames, Audit audit) {
        if (paramNames == null) {
            return "anonymous";
        }

        // 根据参数名查找
        if (!audit.actorParam().isEmpty()) {
            for (int i = 0; i < paramNames.length; i++) {
                if (audit.actorParam().equals(paramNames[i])) {
                    Object actor = args[i];
                    return actor != null ? actor.toString() : "anonymous";
                }
            }
        }

        return "anonymous";
    }

    /**
     * 从实体中提取ID - 支持多种方式
     */
    private String extractEntityId(Object entity, Object[] args, String[] paramNames, Audit audit) {
        // 1. 优先从方法参数中直接获取ID
        if (!audit.idParam().isEmpty() && paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                if (audit.idParam().equals(paramNames[i])) {
                    Object idValue = args[i];
                    if (idValue != null) {
                        return idValue.toString();
                    }
                }
            }
        }

        // 2. 从实体对象中提取ID
        if (entity != null) {
            return extractIdFromEntity(entity, audit.idField());
        }

        return null;
    }

    /**
     * 从实体对象中提取ID
     */
    private String extractIdFromEntity(Object entity, String idField) {
        try {
            // 处理Record类型
            if (entity.getClass().isRecord()) {
                var components = entity.getClass().getRecordComponents();
                for (var component : components) {
                    if (component.getName().equals(idField)) {
                        Object idValue = component.getAccessor().invoke(entity);
                        return idValue != null ? idValue.toString() : null;
                    }
                }
                log.debug("Record类型中未找到字段: {}", idField);
                return null;
            }

            // 处理普通类 - 先尝试get方法
            try {
                String getterName = "get" + idField.substring(0, 1).toUpperCase() + idField.substring(1);
                var getter = entity.getClass().getMethod(getterName);
                Object idValue = getter.invoke(entity);
                return idValue != null ? idValue.toString() : null;
            } catch (NoSuchMethodException e) {
                // 直接访问字段
                Field field = entity.getClass().getDeclaredField(idField);
                field.setAccessible(true);
                Object idValue = field.get(entity);
                return idValue != null ? idValue.toString() : null;
            }
        } catch (Exception e) {
            log.debug("从实体提取ID失败: {} for {}", idField, entity.getClass().getName());
            return null;
        }
    }

    /**
     * 确定操作类型
     */
    private Audit.ActionType determineActionType(Audit audit, String methodName) {
        if (audit.action() != Audit.ActionType.AUTO) {
            return audit.action();
        }

        String lowerMethodName = methodName.toLowerCase();
        if (lowerMethodName.contains("create") || lowerMethodName.contains("save")) {
            return Audit.ActionType.CREATE;
        } else if (lowerMethodName.contains("delete") || lowerMethodName.contains("remove")) {
            return Audit.ActionType.DELETE;
        } else {
            return Audit.ActionType.UPDATE;
        }
    }

    /**
     * 确定执行后的快照
     */
    private Object determineAfterSnapshot(Object entity, Audit.ActionType actionType, String entityId) {
        switch (actionType) {
            case CREATE:
                return entity; // 创建操作，新实体就是最终状态
            case DELETE:
                return null; // 删除操作，最终状态为null
            case UPDATE:
            default:
                return entity; // 更新操作，使用传入的新实体
        }
    }

    /**
     * 构建存储键
     */
    private String buildKey(Class<?> entityClass, String entityId) {
        return entityClass.getSimpleName() + ":" + entityId;
    }

    /**
     * 记录审计日志
     */
    private void recordAudit(String entityType, String entityId, String action,
                           String actor, String diffJson) {
        AuditLog auditLog = new AuditLog(
                Long.toString(auditSequence.incrementAndGet()),
                entityType,
                entityId,
                action,
                actor,
                Instant.now(),
                diffJson
        );

        auditTimeline.add(auditLog);
        auditByEntity.computeIfAbsent(entityId, key -> new CopyOnWriteArrayList<>())
                .add(auditLog);

        log.debug("记录审计日志: {} {} by {}", action, entityType + ":" + entityId, actor);
    }

    /**
     * 查询指定实体的审计日志
     */
    public List<AuditLog> findAuditByEntityId(String entityId) {
        return List.copyOf(auditByEntity.getOrDefault(entityId, List.of()));
    }

    /**
     * 查询所有审计日志
     */
    public List<AuditLog> findAllAudits() {
        return List.copyOf(auditTimeline);
    }
}