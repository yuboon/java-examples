package com.example.executor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * 隔离类加载器
 * 为每个函数提供独立的类加载环境
 */
public class IsolatedClassLoader extends URLClassLoader {
    
    private final String functionName;
    private final Map<String, Class<?>> loadedClasses = new HashMap<>();
    private final ClassLoader parentClassLoader;
    
    public IsolatedClassLoader(String functionName, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.functionName = functionName;
        this.parentClassLoader = parent;
    }
    
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // 检查是否已经加载过
        Class<?> loadedClass = loadedClasses.get(name);
        if (loadedClass != null) {
            return loadedClass;
        }

        // 优先使用父加载器加载基础类
        if (name.startsWith("com.example.model.")) {
            return super.loadClass(name, resolve);
        }

        // 对于Java系统类，使用父类加载器
        if (name.startsWith("java.") || name.startsWith("javax.") || 
            name.startsWith("sun.") || name.startsWith("com.sun.")) {
            return super.loadClass(name, resolve);
        }
        
        // 对于Spring相关类，使用父类加载器
        if (name.startsWith("org.springframework.") || 
            name.startsWith("org.apache.") ||
            name.startsWith("com.fasterxml.")) {
            return super.loadClass(name, resolve);
        }
        
        try {
            // 尝试自己加载类
            Class<?> clazz = findClass(name);
            loadedClasses.put(name, clazz);
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        } catch (ClassNotFoundException e) {
            // 如果找不到，使用父类加载器
            return super.loadClass(name, resolve);
        }
    }
    
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            String path = name.replace('.', '/') + ".class";
            InputStream is = getResourceAsStream(path);
            if (is == null) {
                throw new ClassNotFoundException(name);
            }
            
            byte[] classData = readClassData(is);
            return defineClass(name, classData, 0, classData.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }
    
    private byte[] readClassData(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int bytesRead;
        while ((bytesRead = is.read(data)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toByteArray();
    }
    
    public String getFunctionName() {
        return functionName;
    }
    
    public int getLoadedClassCount() {
        return loadedClasses.size();
    }
    
    @Override
    public void close() throws IOException {
        loadedClasses.clear();
        super.close();
    }
}