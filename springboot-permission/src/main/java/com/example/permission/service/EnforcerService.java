package com.example.permission.service;

import com.example.permission.entity.Document;
import com.example.permission.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.persist.file_adapter.FileAdapter;
import org.casbin.jcasbin.util.function.CustomFunction;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Casbin 权限决策服务
 */
@Slf4j
@Service
public class EnforcerService {

    private Enforcer enforcer;

    @PostConstruct
    public void init() {
        try {
            String modelPath = "src/main/resources/casbin/model.conf";
            String policyPath = "src/main/resources/casbin/policy.csv";

            Model model = new Model();
            model.loadModel(modelPath);

            enforcer = new Enforcer(model, new FileAdapter(policyPath));

            log.info("Casbin Enforcer 初始化成功");
        } catch (Exception e) {
            log.error("Casbin Enforcer 初始化失败", e);
            throw new RuntimeException("权限引擎初始化失败", e);
        }
    }

    /**
     * 权限判断
     */
    public boolean enforce(User user, Document doc, String action) {
        try {
            // 构建请求上下文
            RequestContext context = new RequestContext(user, doc, action);

            // 执行权限检查
            boolean result = enforcer.enforce(user,doc,action);

            log.debug("权限检查：user={}, doc={}, action={}, result={}",
                    user.getId(), doc.getId(), action, result);

            return result;
        } catch (Exception e) {
            log.error("权限检查异常", e);
            return false;
        }
    }

    /**
     * 添加策略
     */
    public boolean addPolicy(String subRule, String objRule, String act) {
        return enforcer.addPolicy(subRule, objRule, act);
    }

    /**
     * 删除策略
     */
    public boolean removePolicy(String subRule, String objRule, String act) {
        return enforcer.removePolicy(subRule, objRule, act);
    }

    /**
     * 获取所有策略
     */
    public List<List<String>> getAllPolicy() {
        return enforcer.getPolicy();
    }

    /**
     * 保存策略到文件
     */
    public void savePolicy() {
        enforcer.savePolicy();
    }

    /**
     * 请求上下文（封装 ABAC 属性）
     */
    public static class RequestContext {
        public final Map<String, Object> sub;
        public final Map<String, Object> obj;
        public final String act;

        public RequestContext(User user, Document doc, String action) {
            this.sub = user.toAttributes();
            this.obj = doc.toAttributes();
            this.act = action;
        }
    }

}
