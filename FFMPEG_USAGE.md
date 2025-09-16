# FFmpeg H.265 视频转换功能说明

## 功能介绍

本应用现在支持使用内嵌或系统安装的FFmpeg将视频文件转换为H.265（HEVC）编码格式，可以有效减小视频文件大小同时保持良好的画质。

## 使用方法

1. **选择视频文件**：点击"选择文件"按钮或直接拖拽视频文件到应用程序窗口
2. **选择转换H265**：在操作面板中点击"转换H265"按钮
3. **等待处理完成**：程序会自动使用FFmpeg进行视频转换
4. **导出结果**：转换完成后，点击"导出"按钮保存转换后的视频文件

## FFmpeg 部署选项

### 选项1：使用系统安装的FFmpeg（推荐）

如果您的系统已安装FFmpeg，应用程序会自动检测并使用：

**macOS:**
```bash
# 使用Homebrew安装
brew install ffmpeg
```

**Windows:**
1. 从 https://ffmpeg.org/download.html 下载FFmpeg
2. 解压到C:\ffmpeg目录
3. 将C:\ffmpeg\bin添加到系统PATH环境变量

**Linux:**
```bash
# Ubuntu/Debian
sudo apt update && sudo apt install ffmpeg

# CentOS/RHEL
sudo yum install ffmpeg
```

### 选项2：内嵌FFmpeg二进制文件

如果要将FFmpeg内嵌到应用程序中，请将FFmpeg二进制文件放置到以下目录：

```
src/main/resources/ffmpeg/
├── macos/
│   └── ffmpeg           # macOS FFmpeg可执行文件
├── windows/
│   └── ffmpeg.exe       # Windows FFmpeg可执行文件
└── linux/
    └── ffmpeg           # Linux FFmpeg可执行文件
```

**获取FFmpeg二进制文件：**
- 官方下载：https://ffmpeg.org/download.html
- 确保下载的是静态编译版本（static builds）
- macOS和Linux版本需要设置可执行权限

## 转换参数说明

- **编码器**：libx265（H.265/HEVC）
- **质量**：CRF 23（平衡质量与文件大小）
- **预设**：medium（平衡编码速度与压缩率）
- **音频**：保持原始音频编码（copy模式）

## 支持的视频格式

**输入格式**：MP4, AVI, MOV, MKV, WMV, FLV等常见视频格式
**输出格式**：MP4（H.265编码）

## 注意事项

1. **处理时间**：H.265编码比较耗时，大文件可能需要较长时间处理
2. **文件大小**：H.265可显著减小文件大小，通常比H.264小30-50%
3. **兼容性**：H.265格式在较老设备上可能不支持播放
4. **内存使用**：处理大视频文件时会占用较多内存

## 常见问题

**Q: 提示"未找到FFmpeg"错误？**
A: 请确保系统已安装FFmpeg或在resources目录中放置FFmpeg二进制文件

**Q: 转换速度很慢？**
A: H.265编码需要大量计算，这是正常现象。可以考虑关闭其他程序释放CPU资源

**Q: 转换后画质变差？**
A: 可以修改FFmpegUtil.java中的CRF值（降低数值提高质量，但会增加文件大小）

**Q: 支持批量转换吗？**
A: 目前版本支持单文件转换，转换完成后可点击"下一个"继续处理其他文件
