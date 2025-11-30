package com.example.mutualcert.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomX509Validator customX509Validator;

    public SecurityConfig() {
        this.customX509Validator = new CustomX509Validator();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/public/**", "/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated()
            )
            .x509(x509 -> x509
                .subjectPrincipalRegex("CN=(.*?)(?:,|$)")
                .userDetailsService(userDetailsService())
            )
            .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            // 首先检查证书中的CN字段是否在允许的列表中
            if (isAllowedCertificateUser(username)) {
                if ("DemoClient".equals(username)) {
                    return new User(username, "", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
                } else if ("localhost".equals(username)) {
                    return new User(username, "", Collections.singletonList(new SimpleGrantedAuthority("ROLE_SERVER")));
                }
            }
            throw new UsernameNotFoundException("User not found: " + username);
        };
    }

    /**
     * 检查证书用户是否在允许列表中
     */
    private boolean isAllowedCertificateUser(String username) {
        // 这里可以结合 CustomX509Validator 来进行更严格的验证
        // 目前暂时允许 DemoClient 和 localhost
        return "DemoClient".equals(username) || "localhost".equals(username);
    }

    /**
     * 提供自定义证书验证器给其他组件使用
     */
    @Bean
    public CustomX509Validator customX509Validator() {
        return customX509Validator;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}