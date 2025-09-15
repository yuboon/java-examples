package com.example.jarconflict.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ClassLoaderAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ClassLoaderAdapter.class);

    public List<URL> getClasspathUrls() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        logger.info("Detecting ClassLoader type: {}", classLoader.getClass().getName());
        
        List<URL> urls = new ArrayList<>();
        
        // 优先尝试获取所有ClassLoader的URLs
        ClassLoader current = classLoader;
        while (current != null) {
            if (current instanceof URLClassLoader urlClassLoader) {
                urls.addAll(Arrays.asList(urlClassLoader.getURLs()));
            }
            current = current.getParent();
        }
        
        // 如果是Spring Boot LaunchedURLClassLoader，需要特殊处理
        if (classLoader.getClass().getName().contains("LaunchedURLClassLoader")) {
            urls.addAll(extractFromLaunchedClassLoader(classLoader));
        }
        
        // 添加系统类路径作为补充
        urls.addAll(getClasspathFromSystemProperty());
        
        // 去重
        return urls.stream().distinct().collect(ArrayList::new, (list, url) -> {
            if (!list.contains(url)) {
                list.add(url);
            }
        }, ArrayList::addAll);
    }

    private List<URL> extractFromLaunchedClassLoader(ClassLoader classLoader) {
        List<URL> urls = new ArrayList<>();
        try {
            var field = classLoader.getClass().getDeclaredField("classLoader");
            field.setAccessible(true);
            var nestedClassLoader = field.get(classLoader);
            
            if (nestedClassLoader instanceof URLClassLoader urlClassLoader) {
                urls.addAll(Arrays.asList(urlClassLoader.getURLs()));
            }
            
            var urlsField = classLoader.getClass().getDeclaredField("urls");
            if (urlsField != null) {
                urlsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                var urlsList = (List<URL>) urlsField.get(classLoader);
                if (urlsList != null) {
                    urls.addAll(urlsList);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract URLs from LaunchedURLClassLoader, fallback to system property", e);
            return getClasspathFromSystemProperty();
        }
        return urls;
    }

    private List<URL> getClasspathFromSystemProperty() {
        List<URL> urls = new ArrayList<>();
        String classpath = System.getProperty("java.class.path");
        
        if (classpath != null) {
            String[] paths = classpath.split(File.pathSeparator);
            for (String path : paths) {
                try {
                    File file = new File(path);
                    urls.add(file.toURI().toURL());
                } catch (Exception e) {
                    logger.debug("Failed to convert path to URL: {}", path, e);
                }
            }
        }
        return urls;
    }

    public boolean isSpringBootFatJar(String jarPath) {
        return jarPath.contains("BOOT-INF") || jarPath.endsWith("jar!/");
    }

    public boolean isDevelopmentEnvironment() {
        return System.getProperty("spring.profiles.active", "").contains("dev") ||
               System.getProperty("java.class.path", "").contains("target/classes");
    }
}