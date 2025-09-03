# 补丁打包脚本示例

本目录包含了如何将补丁类打包为JAR文件的示例脚本。

## 打包Spring Bean补丁

```bash
#!/bin/bash

# 设置变量
PATCH_NAME="UserService"
VERSION="1.0.1"
PATCH_CLASS="com.example.hotpatch.patches.UserServicePatch"

# 编译补丁类
echo "编译补丁类: $PATCH_CLASS"
# windows
javac  -encoding UTF-8 -cp "target/classes;target/lib/*" -d temp src/main/java/com/example/hotpatch/patches/UserServicePatch.java
# linux
javac  -encoding UTF-8 -cp "target/classes:target/lib/*" -d temp src/main/java/com/example/hotpatch/patches/UserServicePatch.java

# 打包为JAR
echo "打包补丁: $PATCH_NAME-$VERSION.jar"
jar cf $PATCH_NAME-$VERSION.jar -C ./temp .

# 清理临时文件
rm -rf ./temp

echo "✅ 补丁打包完成: $PATCH_NAME-$VERSION.jar"
```

## 批量打包所有补丁

```bash
#!/bin/bash

PATCHES_DIR="../src/main/java/com/example/hotpatch/patches"
OUTPUT_DIR="."

# 创建临时目录
mkdir -p temp

# 编译所有补丁类
echo "编译所有补丁类..."
javac -cp "../target/classes:../target/lib/*" \
      -d ./temp \
      $PATCHES_DIR/*.java

# 为每个补丁类创建单独的JAR
for patch_file in $PATCHES_DIR/*.java; do
    filename=$(basename "$patch_file" .java)
    
    # 提取版本信息（从注解中）
    version=$(grep -o 'version = "[^"]*"' "$patch_file" | sed 's/version = "//;s/"//')
    if [ -z "$version" ]; then
        version="1.0.0"
    fi
    
    # 创建补丁JAR
    patch_name=$(echo $filename | sed 's/Patch$//')
    jar_name="$patch_name-$version.jar"
    
    echo "打包补丁: $jar_name"
    jar cf $jar_name -C ./temp com/example/hotpatch/patches/$filename.class
done

# 清理
rm -rf temp

echo "✅ 所有补丁打包完成"
```