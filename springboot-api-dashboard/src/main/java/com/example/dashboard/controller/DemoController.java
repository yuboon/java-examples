package com.example.dashboard.controller;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/demo")
public class DemoController {
    
    private final Random random = new Random();

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        simulateRandomDelay(100, 300);

        // 模拟错误
        int s = 1 / 0;

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello from Spring Boot API Dashboard!");
        response.put("timestamp", System.currentTimeMillis());
        response.put("status", "success");
        
        return response;
    }

    @GetMapping("/users")
    public Map<String, Object> getUsers() {
        simulateRandomDelay(200, 500);
        
        Map<String, Object> response = new HashMap<>();
        response.put("users", new String[]{"Alice", "Bob", "Charlie", "Diana"});
        response.put("count", 4);
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    @PostMapping("/users")
    public Map<String, Object> createUser(@RequestBody Map<String, String> user) {
        simulateRandomDelay(300, 800);
        
        // Simulate occasional failures
        if (random.nextInt(10) == 0) {
            throw new RuntimeException("Simulated database error");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User created successfully");
        response.put("user", user);
        response.put("id", random.nextInt(1000));
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    @GetMapping("/products")
    public Map<String, Object> getProducts(@RequestParam(defaultValue = "10") int limit) {
        simulateRandomDelay(150, 600);
        
        Map<String, Object> response = new HashMap<>();
        response.put("products", generateProducts(limit));
        response.put("total", limit);
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    @GetMapping("/orders/{orderId}")
    public Map<String, Object> getOrder(@PathVariable String orderId) {
        simulateRandomDelay(100, 400);
        
        // Simulate not found errors
        if (random.nextInt(20) == 0) {
            throw new RuntimeException("Order not found: " + orderId);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("status", "completed");
        response.put("amount", 99.99);
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    @GetMapping("/slow")
    public Map<String, Object> slowEndpoint() {
        simulateRandomDelay(2000, 5000);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is a slow endpoint for testing");
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    @GetMapping("/error")
    public Map<String, Object> errorEndpoint() {
        simulateRandomDelay(50, 200);
        
        if (random.nextInt(2) == 0) {
            throw new RuntimeException("Intentional error for testing");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sometimes I work, sometimes I don't");
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    private void simulateRandomDelay(int minMs, int maxMs) {
        try {
            int delay = random.nextInt(maxMs - minMs) + minMs;
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String[] generateProducts(int count) {
        String[] products = new String[count];
        String[] names = {"Laptop", "Mouse", "Keyboard", "Monitor", "Phone", "Tablet", "Camera", "Speaker"};
        
        for (int i = 0; i < count; i++) {
            products[i] = names[random.nextInt(names.length)] + " " + (i + 1);
        }
        
        return products;
    }
}