package club.lemos.y7converter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * FFmpeg工具类
 * 负责管理内嵌FFmpeg二进制文件的提取和音频处理
 */
public class FFmpegUtil {

    private static final String TEMP_DIR_PREFIX = "y7converter_ffmpeg_";
    private static File extractedFFmpegPath;

    /**
     * 获取或提取FFmpeg可执行文件路径
     *
     * @return FFmpeg可执行文件的绝对路径
     * @throws IOException 如果提取失败
     */
    public static String getFFmpegPath() throws IOException {
        if (extractedFFmpegPath != null && extractedFFmpegPath.exists()) {
            return extractedFFmpegPath.getAbsolutePath();
        }

        String osName = System.getProperty("os.name").toLowerCase();
        String resourcePath;
        String executableName;

        if (osName.contains("mac")) {
            resourcePath = "/ffmpeg/macos/ffmpeg";
            executableName = "ffmpeg";
        } else if (osName.contains("win")) {
            resourcePath = "/ffmpeg/windows/ffmpeg.exe";
            executableName = "ffmpeg.exe";
        } else {
            resourcePath = "/ffmpeg/linux/ffmpeg";
            executableName = "ffmpeg";
        }

        // 尝试从资源文件提取FFmpeg
        try (InputStream resourceStream = FFmpegUtil.class.getResourceAsStream(resourcePath)) {
            if (resourceStream == null) {
                // 如果没有内嵌的FFmpeg，尝试使用系统FFmpeg
                return getSystemFFmpegPath();
            }

            // 创建临时目录
            Path tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX);
            tempDir.toFile().deleteOnExit();

            // 提取FFmpeg到临时目录
            Path ffmpegFile = tempDir.resolve(executableName);
            Files.copy(resourceStream, ffmpegFile, StandardCopyOption.REPLACE_EXISTING);

            // 设置可执行权限（Unix系统）
            if (!osName.contains("win")) {
                ffmpegFile.toFile().setExecutable(true);
            }

            extractedFFmpegPath = ffmpegFile.toFile();
            extractedFFmpegPath.deleteOnExit();

            return extractedFFmpegPath.getAbsolutePath();
        }
    }

    /**
     * 尝试获取系统安装的FFmpeg路径
     *
     * @return 系统FFmpeg路径，如果未找到则抛出异常
     * @throws IOException 如果系统中没有安装FFmpeg
     */
    private static String getSystemFFmpegPath() throws IOException {
        String osName = System.getProperty("os.name").toLowerCase();
        List<String> possiblePaths = new ArrayList<>();

        if (osName.contains("win")) {
            possiblePaths.add("ffmpeg.exe");
            possiblePaths.add("C:\\ffmpeg\\bin\\ffmpeg.exe");
            possiblePaths.add("C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe");
        } else {
            possiblePaths.add("ffmpeg");
            possiblePaths.add("/usr/bin/ffmpeg");
            possiblePaths.add("/usr/local/bin/ffmpeg");
            possiblePaths.add("/opt/homebrew/bin/ffmpeg");
        }

        for (String path : possiblePaths) {
            try {
                ProcessBuilder pb = new ProcessBuilder(path, "-version");
                Process process = pb.start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    return path;
                }
            } catch (Exception e) {
                // 继续尝试下一个路径
            }
        }

        throw new IOException("未找到FFmpeg！请确保FFmpeg已安装或将FFmpeg二进制文件放入resources/ffmpeg目录中。");
    }

    /**
     * 构建音频提取命令
     *
     * @param ffmpegPath FFmpeg可执行文件路径
     * @param inputFile  输入视频文件
     * @param outputFile 输出音频文件
     * @return 命令列表
     */
    public static List<String> buildAudioExtractCommand(String ffmpegPath, File inputFile, File outputFile) {
        List<String> command = new ArrayList<>();

        command.add(ffmpegPath);
        command.add("-i");
        command.add(inputFile.getAbsolutePath());

        // 提取音频，转换为ACC格式（语音识别常用格式）
        // 不包含视频流
        command.add("-vn");
        // 指定音频编码器为 AAC
        command.add("-acodec");
        command.add("aac");  // 使用 AAC 编码器
        // 设置采样率、声道数（保持语音识别兼容性）
        command.add("-ar");
        command.add("16000");  // 16kHz 采样率
        command.add("-ac");
        command.add("1");      // 单声道
        // 可选：指定比特率（如 64k，语音识别一般够用）
        command.add("-b:a");
        command.add("64k");
        // 覆盖输出文件
        command.add("-y");

        // 输出文件
        command.add(outputFile.getAbsolutePath());

        return command;
    }

    /**
     * 清理临时文件
     */
    public static void cleanup() {
        if (extractedFFmpegPath != null && extractedFFmpegPath.exists()) {
            try {
                extractedFFmpegPath.delete();
                // 尝试删除临时目录
                File tempDir = extractedFFmpegPath.getParentFile();
                if (tempDir != null && tempDir.getName().startsWith(TEMP_DIR_PREFIX)) {
                    tempDir.delete();
                }
            } catch (Exception e) {
                // 忽略清理错误
            }
        }
    }
}