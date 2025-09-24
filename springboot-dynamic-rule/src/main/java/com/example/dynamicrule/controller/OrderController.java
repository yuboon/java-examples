package com.example.dynamicrule.controller;

import com.example.dynamicrule.entity.Order;
import com.example.dynamicrule.entity.OrderProcessResult;
import com.example.dynamicrule.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/process")
    public ResponseEntity<OrderProcessResult> processOrder(@RequestBody Order order) {
        try {
            OrderProcessResult processedOrder = orderService.processOrder(order);
            return ResponseEntity.ok(processedOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/simulate")
    public ResponseEntity<OrderProcessResult> simulateOrder(
            @RequestParam String userLevel,
            @RequestParam BigDecimal amount) {
        try {
            Order sampleOrder = orderService.createSampleOrder(userLevel, amount);
            OrderProcessResult processedOrder = orderService.processOrder(sampleOrder);
            return ResponseEntity.ok(processedOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}