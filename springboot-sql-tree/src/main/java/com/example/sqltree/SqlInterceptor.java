package com.example.sqltree;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * MyBatis SQL拦截器
 * 拦截SQL执行过程，构建SQL调用树
 */
@Slf4j
@Component
@Intercepts({
    @Signature(type = Executor.class, method = "query", args = {
        MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class
    }),
    @Signature(type = Executor.class, method = "update", args = {
        MappedStatement.class, Object.class
    }),
    @Signature(type = StatementHandler.class, method = "prepare", args = {
        Connection.class, Integer.class
    })
})
public class SqlInterceptor implements Interceptor {
    
    @Autowired
    private SqlCallTreeContext sqlCallTreeContext;
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 检查是否启用追踪
        if (!sqlCallTreeContext.isTraceEnabled()) {
            return invocation.proceed();
        }
        
        Object target = invocation.getTarget();
        
        if (target instanceof Executor) {
            return interceptExecutor(invocation);
        } else if (target instanceof StatementHandler) {
            return interceptStatementHandler(invocation);
        }
        
        return invocation.proceed();
    }
    
    /**
     * 拦截Executor执行
     * @param invocation 调用信息
     * @return 执行结果
     * @throws Throwable 异常
     */
    private Object interceptExecutor(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) args[0];
        Object parameter = args[1];
        
        // 获取SQL信息
        BoundSql boundSql = mappedStatement.getBoundSql(parameter);
        String sql = boundSql.getSql();
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
        
        // 创建SQL节点
        SqlNode sqlNode = createSqlNode(sql, sqlCommandType.name(), boundSql, parameter);
        
        Object result = null;
        String errorMessage = null;
        int affectedRows = 0;
        
        try {
            // 执行SQL
            result = invocation.proceed();
            
            // 计算影响行数
            if (result instanceof List) {
                affectedRows = ((List<?>) result).size();
            } else if (result instanceof Integer) {
                affectedRows = (Integer) result;
            }
            
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error("SQL执行异常: {}", sql, e);
            throw e;
        } finally {
            // 退出SQL调用
            sqlCallTreeContext.exit(sqlNode, affectedRows, errorMessage);
        }
        
        return result;
    }
    
    /**
     * 拦截StatementHandler准备
     * @param invocation 调用信息
     * @return 执行结果
     * @throws Throwable 异常
     */
    private Object interceptStatementHandler(Invocation invocation) throws Throwable {
        try {
            StatementHandler statementHandler = getStatementHandler(invocation);
            if (statementHandler != null) {
                BoundSql boundSql = statementHandler.getBoundSql();
                String sql = boundSql.getSql();
                
                log.debug("StatementHandler准备SQL: {}", sql);
            }
        } catch (Exception e) {
            log.warn("拦截StatementHandler时发生异常", e);
        }
        
        return invocation.proceed();
    }
    
    /**
     * 获取StatementHandler实例
     * @param invocation 调用信息
     * @return StatementHandler实例
     */
    private StatementHandler getStatementHandler(Invocation invocation) {
        try {
            Object target = invocation.getTarget();
            if (target instanceof StatementHandler) {
                return (StatementHandler) target;
            }
            
            // 处理代理对象
            MetaObject metaObject = SystemMetaObject.forObject(target);
            while (metaObject.hasGetter("h")) {
                Object object = metaObject.getValue("h");
                if (object instanceof StatementHandler) {
                    return (StatementHandler) object;
                }
                metaObject = SystemMetaObject.forObject(object);
            }
            
            while (metaObject.hasGetter("target")) {
                Object object = metaObject.getValue("target");
                if (object instanceof StatementHandler) {
                    return (StatementHandler) object;
                }
                metaObject = SystemMetaObject.forObject(object);
            }
            
        } catch (Exception e) {
            log.warn("获取StatementHandler实例时发生异常", e);
        }
        
        return null;
    }
    
    /**
     * 创建SQL节点
     * @param sql SQL语句
     * @param sqlType SQL类型
     * @param boundSql BoundSql对象
     * @param parameter 参数对象
     * @return SQL节点
     */
    private SqlNode createSqlNode(String sql, String sqlType, BoundSql boundSql, Object parameter) {
        try {
            // 进入SQL调用
            SqlNode sqlNode = sqlCallTreeContext.enter(sql, sqlType);
            
            if (sqlNode != null) {
                // 提取SQL参数
                List<Object> parameters = extractParameters(boundSql, parameter);
                sqlNode.setParameters(parameters);
                
                // 格式化SQL
                String formattedSql = formatSqlWithParameters(sql, parameters);
                sqlNode.setFormattedSql(formattedSql);
                
                log.debug("创建SQL节点: type={}, depth={}, sql={}", 
                         sqlType, sqlNode.getDepth(), sql);
            }
            
            return sqlNode;
            
        } catch (Exception e) {
            log.error("创建SQL节点时发生异常", e);
            return null;
        }
    }
    
    /**
     * 提取SQL参数
     * @param boundSql BoundSql对象
     * @param parameter 参数对象
     * @return 参数列表
     */
    private List<Object> extractParameters(BoundSql boundSql, Object parameter) {
        List<Object> parameters = new ArrayList<>();
        
        try {
            List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
            if (parameterMappings != null && !parameterMappings.isEmpty() && parameter != null) {
                // 使用 SystemMetaObject 创建 MetaObject，避免依赖 Configuration
                MetaObject metaObject = SystemMetaObject.forObject(parameter);
                
                for (ParameterMapping parameterMapping : parameterMappings) {
                    String propertyName = parameterMapping.getProperty();
                    Object value;
                    
                    if (metaObject.hasGetter(propertyName)) {
                        value = metaObject.getValue(propertyName);
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        value = boundSql.getAdditionalParameter(propertyName);
                    } else {
                        value = null;
                    }
                    
                    parameters.add(value);
                }
            }
        } catch (Exception e) {
            log.warn("提取SQL参数时发生异常", e);
        }
        
        return parameters;
    }
    
    /**
     * 格式化SQL语句，替换参数占位符
     * @param sql 原始SQL
     * @param parameters 参数列表
     * @return 格式化后的SQL
     */
    private String formatSqlWithParameters(String sql, List<Object> parameters) {
        if (sql == null || parameters == null || parameters.isEmpty()) {
            return sql;
        }
        
        try {
            String formattedSql = sql;
            
            for (Object parameter : parameters) {
                String paramValue;
                if (parameter == null) {
                    paramValue = "NULL";
                } else if (parameter instanceof String) {
                    paramValue = "'" + parameter.toString().replace("'", "''") + "'";
                } else if (parameter instanceof java.util.Date) {
                    paramValue = "'" + parameter.toString() + "'";
                } else {
                    paramValue = parameter.toString();
                }
                
                formattedSql = formattedSql.replaceFirst("\\?", paramValue);
            }
            
            return formattedSql;
            
        } catch (Exception e) {
            log.warn("格式化SQL时发生异常", e);
            return sql;
        }
    }
    
    /**
     * 记录执行结果
     * @param sqlNode SQL节点
     * @param result 执行结果
     * @param error 错误信息
     */
    private void recordExecutionResult(SqlNode sqlNode, Object result, Throwable error) {
        if (sqlNode == null) {
            return;
        }
        
        try {
            int affectedRows = 0;
            String errorMessage = null;
            
            if (error != null) {
                errorMessage = error.getMessage();
            } else if (result != null) {
                if (result instanceof List) {
                    affectedRows = ((List<?>) result).size();
                } else if (result instanceof Integer) {
                    affectedRows = (Integer) result;
                }
            }
            
            sqlCallTreeContext.exit(sqlNode, affectedRows, errorMessage);
            
        } catch (Exception e) {
            log.error("记录执行结果时发生异常", e);
        }
    }
    
    @Override
    public Object plugin(Object target) {
        // 只拦截Executor和StatementHandler
        if (target instanceof Executor || target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }
    
    @Override
    public void setProperties(Properties properties) {
        // 可以通过properties配置拦截器参数
        if (properties != null) {
            String slowSqlThreshold = properties.getProperty("slowSqlThreshold");
            if (slowSqlThreshold != null && !slowSqlThreshold.trim().isEmpty()) {
                try {
                    long threshold = Long.parseLong(slowSqlThreshold);
                    if (sqlCallTreeContext != null) {
                        sqlCallTreeContext.setSlowSqlThreshold(threshold);
                    }
                    log.info("设置慢SQL阈值: {}ms", threshold);
                } catch (NumberFormatException e) {
                    log.warn("慢SQL阈值配置格式错误: {}", slowSqlThreshold);
                }
            }
            
            String traceEnabled = properties.getProperty("traceEnabled");
            if (traceEnabled != null && !traceEnabled.trim().isEmpty()) {
                boolean enabled = Boolean.parseBoolean(traceEnabled);
                if (sqlCallTreeContext != null) {
                    sqlCallTreeContext.setTraceEnabled(enabled);
                }
                log.info("设置SQL追踪状态: {}", enabled ? "启用" : "禁用");
            }
        }
    }
}