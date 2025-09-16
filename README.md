## y7converter2

生成应用图标
```bash
png2icons icon.png src/main/resources/icon -allwe
```

精简运行时
```bash
jlink --module-path $JAVA_HOME/jmods \
      --add-modules java.base,java.desktop,java.logging \
      --strip-debug \
      --no-header-files \
      --no-man-pages \
      --output runtime
```
