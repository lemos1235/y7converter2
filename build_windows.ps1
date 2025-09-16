#!/bin/bash
# 清理
./gradlew clean

# 构建
./gradlew assemble

# 打包，参考 https://github.com/wixtoolset/wix3/releases
jpackage --name "Y7" `
         --input build/libs `
         --dest dist `
         --main-jar y7converter2-1.0.jar `
         --main-class club.lemos.App `
         --type msi `
         --icon src/main/resources/icon.ico `
         --app-version 1.0.0 `
         --vendor "lg" `
         --runtime-image runtime `
         --win-menu `
         --win-shortcut
