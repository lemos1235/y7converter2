package club.lemos.y7converter;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 字幕生成命令执行工具类
 */
public class CommandExecutor {

    /**
     * 执行命令行操作
     *
     * @param action     操作类型
     * @param sourceFile 源文件
     * @param destFile   目标文件
     * @return 命令执行结果，包含处理时间信息
     */
    public static CommandResult execute(int action, File sourceFile, File destFile) {
        try {
            long startTime = System.currentTimeMillis();
            
            // 对于生成字幕操作，需要特殊处理
            if (action == CommandActions.GENERATE_SUBTITLE) {
                return executeSubtitleGeneration(sourceFile, destFile, startTime);
            }
            
            // 对于翻译字幕操作，需要特殊处理
            if (action == CommandActions.TRANSLATE_SUBTITLE) {
                return executeSubtitleTranslation(sourceFile, destFile, startTime);
            }
            
            List<String> command = buildCommand(action, sourceFile, destFile);
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(sourceFile.getParentFile()); // 设置工作目录为源文件所在目录
            
            Process process = processBuilder.start();
            
            // 读取输出和错误流
            String output = readStream(process.getInputStream());
            String error = readStream(process.getErrorStream());
            
            // 根据操作类型设置不同的超时时间
            int timeoutSeconds = getTimeoutForAction(action);
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("命令执行超时");
            }
            
            int exitCode = process.exitValue();
            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;
            
            if (exitCode != 0) {
                throw new RuntimeException("命令执行失败: " + error);
            }
            
            return new CommandResult(destFile, processingTime, action, output, error, exitCode);
        } catch (Exception e) {
            throw new RuntimeException("执行命令时发生错误: " + e.getMessage(), e);
        }
    }
    
    /**
     * 执行字幕生成操作（音频提取 + 语音识别）
     */
    private static CommandResult executeSubtitleGeneration(File sourceFile, File destFile, long startTime) throws Exception {
        // 第一步：提取音频
        File tempAudioFile = Files.createTempFile("extracted_audio_", ".aac").toFile();
        tempAudioFile.deleteOnExit();
        
        try {
            // 执行音频提取
            CommandResult audioResult = execute(CommandActions.EXTRACT_AUDIO, sourceFile, tempAudioFile);
            
            if (!audioResult.isSuccess() || !tempAudioFile.exists()) {
                throw new RuntimeException("音频提取失败: " + audioResult.getError());
            }
            
            // 第二步：语音识别（自动上传音频文件并进行识别）
            String subtitleContent = performSpeechRecognition(tempAudioFile);
            
            // 第三步：保存字幕文件
            try (java.io.FileWriter writer = new java.io.FileWriter(destFile)) {
                writer.write(subtitleContent);
            }
            
            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;
            
            String successMessage = "字幕生成成功\n音频文件: " + tempAudioFile.getAbsolutePath() + 
                                   "\n字幕文件: " + destFile.getAbsolutePath();
            
            return new CommandResult(destFile, processingTime, CommandActions.GENERATE_SUBTITLE, 
                                   successMessage, "", 0);
            
        } finally {
            // 清理临时音频文件（如果语音识别过程中未删除）
            if (tempAudioFile.exists()) {
                if (ConfigLoader.getInstance().isDebugMode()) {
                    System.out.println("清理剩余的临时音频文件: " + tempAudioFile.getAbsolutePath());
                }
                tempAudioFile.delete();
            }
        }
    }
    
    /**
     * 执行字幕翻译操作
     */
    private static CommandResult executeSubtitleTranslation(File sourceFile, File destFile, long startTime) throws Exception {
        try {
            // 检查翻译API Key是否配置
            if (!SubtitleTranslationService.isTranslationApiKeyConfigured()) {
                throw new RuntimeException("未配置通义千问翻译API Key。请在配置文件中设置 translation.dashscope.api_key 或设置环境变量 DASHSCOPE_API_KEY");
            }
            
            System.out.println("开始字幕翻译流程...");
            System.out.println("输入字幕文件: " + sourceFile.getAbsolutePath());
            
            // 获取默认语言配置
            ConfigLoader config = ConfigLoader.getInstance();
            String sourceLang = config.getDefaultSourceLanguage();
            String targetLang = config.getDefaultTargetLanguage();
            
            // 执行翻译
            String translationResult = SubtitleTranslationService.translateSubtitleFile(
                    sourceFile, destFile, sourceLang, targetLang);
            
            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;
            
            String successMessage = "字幕翻译成功\n" + translationResult + 
                                   "\n输入文件: " + sourceFile.getAbsolutePath() + 
                                   "\n输出文件: " + destFile.getAbsolutePath();
            
            System.out.println("字幕翻译完成: " + translationResult);
            
            return new CommandResult(destFile, processingTime, CommandActions.TRANSLATE_SUBTITLE, 
                                   successMessage, "", 0);
            
        } catch (Exception e) {
            // 提供详细的错误信息和解决建议
            String errorMessage = "字幕翻译失败: " + e.getMessage();
            
            if (e.getMessage().contains("API Key")) {
                errorMessage += "\n\n请在配置文件config.yaml中设置通义千问翻译API Key，或设置环境变量DASHSCOPE_API_KEY";
            } else if (e.getMessage().contains("格式")) {
                errorMessage += "\n\n请确保输入的是有效的SRT字幕文件";
            }
            
            throw new RuntimeException(errorMessage, e);
        }
    }
    
    /**
     * 执行语音识别
     */
    private static String performSpeechRecognition(File audioFile) throws Exception {
        // 检查API Key是否配置
        if (!SpeechRecognitionService.isApiKeyConfigured()) {
            throw new RuntimeException("未配置DashScope API Key。请在配置文件中设置 speech_recognition.dashscope.api_key 或设置环境变量 DASHSCOPE_API_KEY");
        }
        
        System.out.println("开始语音识别流程...");
        System.out.println("音频文件: " + audioFile.getAbsolutePath());
        System.out.println("文件大小: " + audioFile.length() + " 字节");
        
        FileUploadService uploadService = null;
        try {
            // 第一步：将音频文件上传到网络
            System.out.println("正在上传音频文件到云存储...");
            uploadService = new FileUploadService();
            FileUploadService.UploadResult uploadResult = uploadService.uploadFile(audioFile);
            
            if (!uploadResult.isSuccess()) {
                throw new RuntimeException("音频文件上传失败: " + uploadResult.getMessage());
            }
            
            String fileUrl = uploadResult.getFileUrl();
            if (fileUrl == null || fileUrl.trim().isEmpty()) {
                throw new RuntimeException("上传成功但未获取到文件URL。上传结果: " + uploadResult.toString());
            }
            
            System.out.println("音频文件上传成功，URL: " + fileUrl);
            
            // 第二步：执行语音识别
            System.out.println("正在进行语音识别...");
            String transcriptionResult = SpeechRecognitionService.transcribeAudioFromUrl(fileUrl);
            
            if (transcriptionResult.trim().isEmpty()) {
                throw new RuntimeException("语音识别完成但结果为空");
            }
            
            System.out.println("语音识别完成，生成字幕内容长度: " + transcriptionResult.length() + " 字符");
            
            // 第三步：从OSS删除已上传的音频文件
            System.out.println("正在从云存储删除音频文件...");
            if (uploadService.deleteUploadedFile(uploadResult)) {
                System.out.println("云存储中的音频文件删除成功");
            } else {
                System.err.println("云存储中的音频文件删除失败，请手动清理");
            }
            
            return transcriptionResult;
            
        } catch (Exception e) {
            // 提供详细的错误信息和解决建议
            String errorMessage = "语音识别失败: " + e.getMessage();
            
            if (e.getMessage().contains("上传")) {
                errorMessage += "\n\n请检查：";
                errorMessage += "\n1. 网络连接是否正常";
                errorMessage += "\n2. 配置文件中的上传URL是否正确";
                errorMessage += "\n3. 认证信息是否有效";
            } else if (e.getMessage().contains("API Key")) {
                errorMessage += "\n\n请在配置文件config.yaml中设置DashScope API Key，或设置环境变量DASHSCOPE_API_KEY";
            }
            
            throw new RuntimeException(errorMessage, e);
        } finally {
            // 确保关闭上传服务
            if (uploadService != null) {
                uploadService.close();
            }
        }
    }
    
    /**
     * 根据操作类型获取超时时间
     */
    private static int getTimeoutForAction(int action) {
        switch (action) {
            case CommandActions.EXTRACT_AUDIO:
                return 120; // 音频提取2分钟超时
            case CommandActions.TRANSLATE_SUBTITLE:
                return 300; // 字幕翻译5分钟超时
            default:
                return 60;  // 默认1分钟超时
        }
    }

    /**
     * 根据操作类型构建命令
     *
     * @param action     操作类型
     * @param sourceFile 源文件
     * @param destFile   目标文件
     * @return 命令列表
     */
    private static List<String> buildCommand(int action, File sourceFile, File destFile) {
        List<String> command;
        
        if (action == CommandActions.EXTRACT_AUDIO) {
            // 使用FFmpeg提取音频
            try {
                String ffmpegPath = FFmpegUtil.getFFmpegPath();
                command = FFmpegUtil.buildAudioExtractCommand(ffmpegPath, sourceFile, destFile);
            } catch (Exception e) {
                throw new RuntimeException("无法获取FFmpeg: " + e.getMessage(), e);
            }
        } else {
            throw new IllegalArgumentException("不支持的操作类型: " + action);
        }
        
        return command;
    }

    /**
     * 读取流内容
     *
     * @param inputStream 输入流
     * @return 流内容
     */
    private static String readStream(InputStream inputStream) throws IOException {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }
}
