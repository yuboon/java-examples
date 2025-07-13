package com.example.unified.invoker;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.example.unified.dto.OrderDTO;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.service.GenericService;

public class Test {

    public static void main(String[] args) {

        ReferenceConfig<GenericService> reference = new ReferenceConfig<>();
        reference.setApplication(new ApplicationConfig("generic-consumer"));
        ApplicationModel applicationModel = ApplicationModel.defaultModel();

        reference.setRegistry(new RegistryConfig("N/A"));
        reference.setVersion("1.0.0");
        reference.setUrl("dubbo://127.0.0.1:20880");
        reference.setInterface("com.example.unified.service.OrderService"); // 服务接口全限定名
        reference.setGeneric(true); // 开启泛化调用

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(22L);

        GenericService genericService = reference.get();
        Object result =  genericService.$invoke(
                "getOrder",
                new String[]{"com.example.unified.dto.OrderDTO"},
                new Object[]{orderDTO}
        );
        System.out.println("result:" + JSONUtil.toJsonPrettyStr(result)); // 输出: "Hello, World!"
    }

}
