package club.lemos.y7converter;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.TranslationOptions;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字幕翻译服务类
 * 使用阿里云通义千问翻译模型进行字幕文件翻译
 */
public class SubtitleTranslationService {

    private static final ConfigLoader config = ConfigLoader.getInstance();
    
    // SRT字幕时间戳的正则表达式
    private static final Pattern SRT_TIMESTAMP_PATTERN = Pattern.compile(
        "^(\\d{2}:\\d{2}:\\d{2},\\d{3}) --> (\\d{2}:\\d{2}:\\d{2},\\d{3})$"
    );
    
    // SRT字幕序号的正则表达式
    private static final Pattern SRT_NUMBER_PATTERN = Pattern.compile("^\\d+$");

    /**
     * 翻译字幕文件
     *
     * @param inputFile  输入的字幕文件
     * @param outputFile 输出的翻译后字幕文件
     * @param sourceLang 源语言
     * @param targetLang 目标语言
     * @return 翻译结果信息
     * @throws Exception 如果翻译失败
     */
    public static String translateSubtitleFile(File inputFile, File outputFile, String sourceLang, String targetLang) throws Exception {
        // 验证API Key
        String apiKey = config.getTranslationApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new Exception("通义千问翻译API Key未配置。请在配置文件中设置 translation.dashscope.api_key 或设置环境变量 DASHSCOPE_API_KEY");
        }

        // 读取并解析SRT字幕文件
        List<SubtitleBlock> subtitleBlocks = parseSrtFile(inputFile);
        
        if (subtitleBlocks.isEmpty()) {
            throw new Exception("字幕文件为空或格式不正确");
        }

        // 翻译字幕内容
        List<SubtitleBlock> translatedBlocks = translateSubtitleBlocks(subtitleBlocks, apiKey, sourceLang, targetLang);
        
        // 保存翻译后的字幕文件
        saveSrtFile(translatedBlocks, outputFile);
        
        return String.format("字幕翻译完成！共翻译了 %d 条字幕\n从 %s 翻译到 %s", 
                           translatedBlocks.size(), sourceLang, targetLang);
    }

    /**
     * 解析SRT字幕文件
     *
     * @param srtFile SRT字幕文件
     * @return 字幕块列表
     */
    private static List<SubtitleBlock> parseSrtFile(File srtFile) throws IOException {
        List<SubtitleBlock> blocks = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(srtFile), StandardCharsets.UTF_8))) {
            
            String line;
            SubtitleBlock currentBlock = null;
            StringBuilder textBuilder = new StringBuilder();
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.isEmpty()) {
                    // 空行表示一个字幕块结束
                    if (currentBlock != null && textBuilder.length() > 0) {
                        currentBlock.text = textBuilder.toString().trim();
                        blocks.add(currentBlock);
                        currentBlock = null;
                        textBuilder.setLength(0);
                    }
                } else if (SRT_NUMBER_PATTERN.matcher(line).matches()) {
                    // 序号行
                    currentBlock = new SubtitleBlock();
                    currentBlock.number = Integer.parseInt(line);
                } else if (SRT_TIMESTAMP_PATTERN.matcher(line).matches()) {
                    // 时间戳行
                    if (currentBlock != null) {
                        currentBlock.timestamp = line;
                    }
                } else {
                    // 文本行
                    if (!textBuilder.isEmpty()) {
                        textBuilder.append("\n");
                    }
                    textBuilder.append(line);
                }
            }
            
            // 处理最后一个字幕块
            if (currentBlock != null && !textBuilder.isEmpty()) {
                currentBlock.text = textBuilder.toString().trim();
                blocks.add(currentBlock);
            }
        }
        
        return blocks;
    }

    /**
     * 翻译字幕块列表
     *
     * @param subtitleBlocks 原始字幕块
     * @param apiKey         API密钥
     * @param sourceLang     源语言
     * @param targetLang     目标语言
     * @return 翻译后的字幕块
     */
    private static List<SubtitleBlock> translateSubtitleBlocks(List<SubtitleBlock> subtitleBlocks, 
                                                              String apiKey, String sourceLang, String targetLang) throws Exception {
        
        List<SubtitleBlock> translatedBlocks = new ArrayList<>();
        
        // 根据配置获取批处理大小，避免一次翻译太多内容
        int batchSize = config.getTranslationBatchSize();
        
        for (int i = 0; i < subtitleBlocks.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, subtitleBlocks.size());
            List<SubtitleBlock> batch = subtitleBlocks.subList(i, endIndex);
            
            // 批量翻译
            List<SubtitleBlock> translatedBatch = translateBatch(batch, apiKey, sourceLang, targetLang);
            translatedBlocks.addAll(translatedBatch);
            
            System.out.printf("翻译进度: %d/%d (%.1f%%)\n", 
                            endIndex, subtitleBlocks.size(), 
                            (double) endIndex / subtitleBlocks.size() * 100);
            
            // 避免频繁调用API，添加短暂延迟
            if (endIndex < subtitleBlocks.size()) {
                try {
                    Thread.sleep(config.getTranslationApiDelay());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new Exception("翻译过程被中断", e);
                }
            }
        }
        
        return translatedBlocks;
    }

    /**
     * 批量翻译字幕块
     */
    private static List<SubtitleBlock> translateBatch(List<SubtitleBlock> batch, 
                                                     String apiKey, String sourceLang, String targetLang) throws Exception {
        
        // 构建翻译内容
        StringBuilder contentBuilder = new StringBuilder();
        for (int i = 0; i < batch.size(); i++) {
            SubtitleBlock block = batch.get(i);
            contentBuilder.append(String.format("[%d] %s", i + 1, block.text));
            if (i < batch.size() - 1) {
                contentBuilder.append("\n");
            }
        }
        
        String content = contentBuilder.toString();
        
        try {
            // 按照用户提供的示例调用通义千问翻译API
            Generation gen = new Generation();
            
            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content(content)
                    .build();
            
            TranslationOptions options = TranslationOptions.builder()
                    .sourceLang(sourceLang)
                    .targetLang(targetLang)
                    .build();
            
            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(config.getTranslationModel())
                    .messages(Collections.singletonList(userMsg))
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .translationOptions(options)
                    .build();
            
            GenerationResult result = gen.call(param);
            
            if (result.getOutput() == null || result.getOutput().getChoices() == null || 
                result.getOutput().getChoices().isEmpty()) {
                throw new Exception("翻译API返回空结果");
            }
            
            String translatedContent = result.getOutput().getChoices().get(0).getMessage().getContent();
            
            // 解析翻译结果
            return parseTranslatedContent(batch, translatedContent);
            
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            throw new Exception("调用翻译API失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析翻译结果内容
     */
    private static List<SubtitleBlock> parseTranslatedContent(List<SubtitleBlock> originalBatch, String translatedContent) {
        List<SubtitleBlock> translatedBlocks = new ArrayList<>();
        String[] lines = translatedContent.split("\n");
        
        Pattern translatedPattern = Pattern.compile("^\\[(\\d+)\\]\\s*(.+)$");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            Matcher matcher = translatedPattern.matcher(line);
            if (matcher.matches()) {
                int index = Integer.parseInt(matcher.group(1)) - 1; // 转换为0基索引
                String translatedText = matcher.group(2).trim();
                
                if (index >= 0 && index < originalBatch.size()) {
                    SubtitleBlock originalBlock = originalBatch.get(index);
                    SubtitleBlock translatedBlock = new SubtitleBlock();
                    translatedBlock.number = originalBlock.number;
                    translatedBlock.timestamp = originalBlock.timestamp;
                    translatedBlock.text = translatedText;
                    translatedBlocks.add(translatedBlock);
                }
            }
        }
        
        // 如果解析失败，使用原始文本
        if (translatedBlocks.size() != originalBatch.size()) {
            System.err.println("警告: 翻译结果解析不完整，将使用原始内容作为备选");
            translatedBlocks.clear();
            
            // 尝试简单分割
            String[] simpleSplit = translatedContent.split("\n");
            for (int i = 0; i < originalBatch.size() && i < simpleSplit.length; i++) {
                SubtitleBlock originalBlock = originalBatch.get(i);
                SubtitleBlock translatedBlock = new SubtitleBlock();
                translatedBlock.number = originalBlock.number;
                translatedBlock.timestamp = originalBlock.timestamp;
                translatedBlock.text = simpleSplit[i].trim();
                translatedBlocks.add(translatedBlock);
            }
        }
        
        return translatedBlocks;
    }

    /**
     * 保存SRT字幕文件
     */
    private static void saveSrtFile(List<SubtitleBlock> blocks, File outputFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            
            for (int i = 0; i < blocks.size(); i++) {
                SubtitleBlock block = blocks.get(i);
                
                writer.write(String.valueOf(block.number));
                writer.newLine();
                writer.write(block.timestamp);
                writer.newLine();
                writer.write(block.text);
                writer.newLine();
                
                // 添加空行分隔（除了最后一个块）
                if (i < blocks.size() - 1) {
                    writer.newLine();
                }
            }
        }
    }

    /**
     * 验证翻译API Key是否已配置
     *
     * @return 如果API Key已配置返回true
     */
    public static boolean isTranslationApiKeyConfigured() {
        String apiKey = config.getTranslationApiKey();
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    /**
     * 字幕块数据结构
     */
    private static class SubtitleBlock {
        int number;       // 序号
        String timestamp; // 时间戳
        String text;      // 文本内容
    }
}