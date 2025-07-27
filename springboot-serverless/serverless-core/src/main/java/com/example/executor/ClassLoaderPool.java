package com.example.executor;

  import org.springframework.stereotype.Component;

  import java.io.File;
  import java.net.URL;
  import java.util.HashMap;
  import java.util.List;
  import java.util.Map;
  import java.util.concurrent.ConcurrentHashMap;
  import java.util.concurrent.Executors;
  import java.util.concurrent.ScheduledExecutorService;
  import java.util.concurrent.TimeUnit;
  import java.util.concurrent.atomic.AtomicLong;
  import java.util.stream.Collectors;

/**
   * ClassLoader池管理器
   * 复用ClassLoader以提高性能，同时支持动态清理
   */
  @Component
  public class ClassLoaderPool {

      private final ConcurrentHashMap<String, PooledClassLoader> pool = new ConcurrentHashMap<>();
      private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

      // 配置参数
      private static final long MAX_IDLE_TIME = 30 * 60 * 1000; // 30分钟无使用则清理
      private static final long CLEANUP_INTERVAL = 10 * 60 * 1000; // 10分钟检查一次

      public ClassLoaderPool() {
          // 启动定期清理任务
          cleanupExecutor.scheduleAtFixedRate(this::cleanupIdleClassLoaders,
              CLEANUP_INTERVAL, CLEANUP_INTERVAL, TimeUnit.MILLISECONDS);
      }

      /**
       * 池化的ClassLoader包装类
       */
      public static class PooledClassLoader {
          private final IsolatedClassLoader classLoader;
          private final AtomicLong lastUsedTime = new AtomicLong(System.currentTimeMillis());
          private final AtomicLong useCount = new AtomicLong(0);
          private final String functionName;
          private final String jarPath;
          private final String version; // jar文件的版本/时间戳

          public PooledClassLoader(String functionName, String jarPath, String version, 
                                 IsolatedClassLoader classLoader) {
              this.functionName = functionName;
              this.jarPath = jarPath;
              this.version = version;
              this.classLoader = classLoader;
          }

          public IsolatedClassLoader getClassLoader() {
              lastUsedTime.set(System.currentTimeMillis());
              useCount.incrementAndGet();
              return classLoader;
          }

          public boolean isExpired() {
              return System.currentTimeMillis() - lastUsedTime.get() > MAX_IDLE_TIME;
          }

          public boolean isVersionChanged() {
              // 检查jar文件是否有更新
              File jarFile = new File(jarPath);
              if (!jarFile.exists()) {
                  return true;
              }

              String currentVersion = String.valueOf(jarFile.lastModified());
              return !version.equals(currentVersion);
          }

          public void close() {
              try {
                  classLoader.close();
              } catch (Exception e) {
                  System.err.println("Error closing ClassLoader for " + functionName + ": " + e.getMessage());
              }
          }

          // Getter方法
          public long getLastUsedTime() { return lastUsedTime.get(); }
          public long getUseCount() { return useCount.get(); }
          public String getFunctionName() { return functionName; }
          public String getVersion() { return version; }
      }

      /**
       * 获取或创建ClassLoader
       */
      public IsolatedClassLoader getClassLoader(String functionName, String jarPath, String className) {
          String key = functionName + ":" + jarPath;

          // 检查jar文件版本
          File jarFile = new File(jarPath);
          if (!jarFile.exists()) {
              throw new IllegalArgumentException("JAR file not found: " + jarPath);
          }

          String currentVersion = String.valueOf(jarFile.lastModified());

          PooledClassLoader pooled = pool.compute(key, (k, existing) -> {
              // 如果不存在或版本已过期，创建新的
              if (existing == null || existing.isVersionChanged() || existing.isExpired()) {
                  if (existing != null) {
                      existing.close(); // 关闭旧的
                  }

                  try {
                      URL jarUrl = jarFile.toURI().toURL();
                      IsolatedClassLoader newClassLoader = new IsolatedClassLoader(
                          functionName,
                          new URL[]{jarUrl},
                          Thread.currentThread().getContextClassLoader()
                      );

                      return new PooledClassLoader(functionName, jarPath, currentVersion, newClassLoader);

                  } catch (Exception e) {
                      throw new RuntimeException("Failed to create ClassLoader for " + functionName, e);
                  }
              }

              return existing;
          });

          return pooled.getClassLoader();
      }

      /**
       * 清理空闲的ClassLoader
       */
      private void cleanupIdleClassLoaders() {
          pool.entrySet().removeIf(entry -> {
              PooledClassLoader pooled = entry.getValue();
              if (pooled.isExpired() || pooled.isVersionChanged()) {
                  pooled.close();
                  System.out.println("Cleaned up ClassLoader for function: " + pooled.getFunctionName());
                  return true;
              }
              return false;
          });
      }

      /**
       * 强制刷新指定函数的ClassLoader
       */
      public void refreshClassLoader(String functionName, String jarPath) {
          String key = functionName + ":" + jarPath;
          PooledClassLoader removed = pool.remove(key);
          if (removed != null) {
              removed.close();
              System.out.println("Refreshed ClassLoader for function: " + functionName);
          }
      }

      /**
       * 获取池状态统计
       */
      public Map<String, Object> getPoolStats() {
          Map<String, Object> stats = new HashMap<>();
          stats.put("totalClassLoaders", pool.size());

          long totalUseCount = pool.values().stream()
              .mapToLong(PooledClassLoader::getUseCount)
              .sum();
          stats.put("totalUseCount", totalUseCount);

          List<Map<String, Object>> details = pool.values().stream()
              .map(pooled -> {
                  Map<String, Object> detail = new HashMap<>();
                  detail.put("functionName", pooled.getFunctionName());
                  detail.put("useCount", pooled.getUseCount());
                  detail.put("lastUsedTime", pooled.getLastUsedTime());
                  detail.put("idleTime", System.currentTimeMillis() - pooled.getLastUsedTime());
                  return detail;
              })
              .collect(Collectors.toList());
          stats.put("details", details);

          return stats;
      }

      /**
       * 关闭所有ClassLoader
       */
      public void shutdown() {
          cleanupExecutor.shutdown();
          pool.values().forEach(PooledClassLoader::close);
          pool.clear();
      }
  }
