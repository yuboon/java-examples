package com.example.unified.controller;

import cn.hutool.core.lang.TypeReference;
import com.example.unified.ApiResponse;
import com.example.unified.UnifiedServiceClient;
import com.example.unified.dto.OrderDTO;
import com.example.unified.dto.UserDTO;
import com.example.unified.service.OrderService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DemoController {

    @Autowired
    private UnifiedServiceClient serviceClient;

    @RequestMapping("/user")
    public ApiResponse<UserDTO> getUser(@RequestBody UserDTO qryUserDTO) {
        ApiResponse<UserDTO> response = serviceClient.call("user-service", "getUser", qryUserDTO, new TypeReference<ApiResponse<UserDTO>>() {});
        return response;
    }

    @RequestMapping("/order")
    public ApiResponse<OrderDTO> getOrder(@RequestBody OrderDTO qryOrderDTO) {
        ApiResponse<OrderDTO> response = serviceClient.call("order-service", "getOrder", qryOrderDTO, new TypeReference<ApiResponse<OrderDTO>>() {});
        String status = response.getData().getStatus();
        System.err.println("status:" + status);
        return response;
    }


    @DubboReference(url = "dubbo://127.0.0.1:20880",version = "1.0.0")
    private OrderService orderService;

    @RequestMapping("/order2")
    public ApiResponse<OrderDTO> getOrder2(@RequestBody OrderDTO qryOrderDTO) {
        OrderDTO order = orderService.getOrder(qryOrderDTO);
        return ApiResponse.success(order);
    }



}
