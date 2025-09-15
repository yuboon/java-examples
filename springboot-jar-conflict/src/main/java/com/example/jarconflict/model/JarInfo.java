package com.example.jarconflict.model;

import java.util.List;

public class JarInfo {
    private String name;
    private String path;
    private String version;
    private String size;
    private List<String> classes;
    
    public JarInfo() {}
    
    public JarInfo(String name, String path, String version, String size) {
        this.name = name;
        this.path = path;
        this.version = version;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public List<String> getClasses() {
        return classes;
    }

    public void setClasses(List<String> classes) {
        this.classes = classes;
    }
}