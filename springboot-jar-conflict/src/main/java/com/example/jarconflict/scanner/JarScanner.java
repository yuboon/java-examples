package com.example.jarconflict.scanner;

import com.example.jarconflict.model.JarInfo;
import com.example.jarconflict.utils.ClassLoaderAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

@Component
public class JarScanner {
    private static final Logger logger = LoggerFactory.getLogger(JarScanner.class);

    @Autowired
    private ClassLoaderAdapter classLoaderAdapter;

    @Value("${scanner.exclude-patterns:}")
    private List<String> excludePatterns;

    @Value("${scanner.include-system-jars:false}")
    private boolean includeSystemJars;

    public List<JarInfo> scanJars() {
        logger.info("Starting jar scanning...");
        List<JarInfo> jars = new ArrayList<>();
        List<URL> urls = classLoaderAdapter.getClasspathUrls();
        
        logger.info("Found {} URLs in classpath", urls.size());

        for (URL url : urls) {
            try {
                String path = url.getPath();
                
                if (shouldExclude(path)) {
                    continue;
                }

                if (path.endsWith(".jar")) {
                    JarInfo jarInfo = scanJarFile(url);
                    if (jarInfo != null) {
                        jars.add(jarInfo);
                    }
                } else if (path.endsWith("/classes/") || path.contains("target/classes")) {
                    JarInfo jarInfo = scanClassesDirectory(url);
                    if (jarInfo != null) {
                        jars.add(jarInfo);
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to scan URL: {}", url, e);
            }
        }

        logger.info("Completed jar scanning, found {} jars", jars.size());
        return jars;
    }

    private JarInfo scanJarFile(URL url) {
        try {
            String path = url.getPath();
            File jarFile = new File(path);
            
            if (!jarFile.exists()) {
                return null;
            }

            try (JarFile jar = new JarFile(jarFile)) {
                JarInfo jarInfo = new JarInfo();
                jarInfo.setPath(path);
                jarInfo.setName(extractJarName(jarFile.getName()));
                jarInfo.setVersion(extractVersion(jar));
                jarInfo.setSize(formatSize(jarFile.length()));
                
                List<String> classes = new ArrayList<>();
                jar.stream()
                    .filter(entry -> entry.getName().endsWith(".class"))
                    .filter(entry -> !entry.getName().contains("$"))
                    .forEach(entry -> {
                        String className = entry.getName()
                            .replace("/", ".")
                            .replace(".class", "");
                        classes.add(className);
                    });
                
                jarInfo.setClasses(classes);
                logger.debug("Scanned jar: {} with {} classes", jarInfo.getName(), classes.size());
                return jarInfo;
            }
        } catch (IOException e) {
            logger.warn("Failed to scan jar file: {}", url, e);
            return null;
        }
    }

    private JarInfo scanClassesDirectory(URL url) {
        try {
            String path = url.getPath();
            File classesDir = new File(path);
            
            if (!classesDir.exists()) {
                return null;
            }

            JarInfo jarInfo = new JarInfo();
            jarInfo.setPath(path);
            jarInfo.setName("classes (development)");
            jarInfo.setVersion("dev");
            jarInfo.setSize(formatSize(calculateDirectorySize(classesDir)));
            
            List<String> classes = new ArrayList<>();
            scanClassesInDirectory(classesDir, "", classes);
            jarInfo.setClasses(classes);
            
            logger.debug("Scanned classes directory with {} classes", classes.size());
            return jarInfo;
        } catch (Exception e) {
            logger.warn("Failed to scan classes directory: {}", url, e);
            return null;
        }
    }

    private void scanClassesInDirectory(File dir, String packagePrefix, List<String> classes) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                String newPackage = packagePrefix.isEmpty() ? 
                    file.getName() : packagePrefix + "." + file.getName();
                scanClassesInDirectory(file, newPackage, classes);
            } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                String className = packagePrefix.isEmpty() ? 
                    file.getName().replace(".class", "") :
                    packagePrefix + "." + file.getName().replace(".class", "");
                classes.add(className);
            }
        }
    }

    private boolean shouldExclude(String path) {
        if (!includeSystemJars && isSystemJar(path)) {
            return true;
        }

        return excludePatterns.stream()
            .anyMatch(pattern -> Pattern.compile(pattern.replace("*", ".*")).matcher(path).matches());
    }

    private boolean isSystemJar(String path) {
        return path.contains("jre/lib") || 
               path.contains("jdk/lib") || 
               path.contains("java.base") ||
               path.startsWith("/modules/");
    }

    private String extractJarName(String fileName) {
        if (fileName.endsWith(".jar")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        
        int dashIndex = fileName.lastIndexOf('-');
        if (dashIndex > 0 && dashIndex < fileName.length() - 1) {
            String versionPart = fileName.substring(dashIndex + 1);
            if (versionPart.matches("\\d+.*")) {
                return fileName.substring(0, dashIndex);
            }
        }
        return fileName;
    }

    private String extractVersion(JarFile jar) {
        try {
            Manifest manifest = jar.getManifest();
            if (manifest != null) {
                String version = manifest.getMainAttributes().getValue("Implementation-Version");
                if (version != null) {
                    return version;
                }
                version = manifest.getMainAttributes().getValue("Bundle-Version");
                if (version != null) {
                    return version;
                }
            }
            
            var pomProperties = jar.getEntry("META-INF/maven");
            if (pomProperties != null) {
                return "maven";
            }
            
            return extractVersionFromFileName(jar.getName());
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String extractVersionFromFileName(String fileName) {
        String baseName = Paths.get(fileName).getFileName().toString();
        if (baseName.endsWith(".jar")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }
        
        int dashIndex = baseName.lastIndexOf('-');
        if (dashIndex > 0 && dashIndex < baseName.length() - 1) {
            String versionPart = baseName.substring(dashIndex + 1);
            if (versionPart.matches("\\d+.*")) {
                return versionPart;
            }
        }
        return "unknown";
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }

    private long calculateDirectorySize(File directory) {
        long size = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += calculateDirectorySize(file);
                }
            }
        }
        return size;
    }
}