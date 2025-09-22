package com.example.staticpermit.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 主页控制器
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            System.out.println("Authentication object: " + auth);
            System.out.println("Principal: " + auth.getPrincipal());
            System.out.println("Name: " + auth.getName());
            System.out.println("Authorities: " + auth.getAuthorities());
            System.out.println("Is authenticated: " + auth.isAuthenticated());

            model.addAttribute("currentUser", auth.getName());
            model.addAttribute("userRoles", auth.getAuthorities());
            model.addAttribute("isAuthenticated", auth.isAuthenticated());
        } else {
            System.out.println("No authentication found");
            model.addAttribute("isAuthenticated", false);
        }
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}