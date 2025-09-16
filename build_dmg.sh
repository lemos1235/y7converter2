#!/bin/bash
# 清理
./gradlew clean

# 构建
./gradlew assemble

# 打包
jpackage --name "Y7" \
         --input build/libs \
         --dest dist \
         --main-jar y7converter2-1.0.jar \
         --main-class club.lemos.App \
         --type dmg \
         --icon src/main/resources/icon.icns \
         --app-version 1.0.0 \
         --vendor "lg"
