package club.lemos.y7converter;

import com.alibaba.dashscope.audio.asr.transcription.*;
import com.alibaba.dashscope.common.TaskStatus;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * 语音识别服务类
 * 使用阿里云DashScope API进行音频转写
 */
public class SpeechRecognitionService {

    private static final ConfigLoader config = ConfigLoader.getInstance();

    /**
     * 使用网络URL进行语音识别
     *
     * @param audioUrl 音频文件的网络URL
     * @return 识别结果字符串
     * @throws Exception 如果识别失败
     */
    public static String transcribeAudioFromUrl(String audioUrl) throws Exception {
        // 获取API Key，优先使用配置文件，否则使用环境变量
        String apiKey = config.getDashScopeApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new Exception("DashScope API Key未配置。请在配置文件中设置 speech_recognition.dashscope.api_key 或设置环境变量 DASHSCOPE_API_KEY");
        }

        // 创建转写请求参数
        TranscriptionParam param = TranscriptionParam.builder()
                .apiKey(apiKey)
                .model(config.getSpeechModel())
                // 支持多语言识别
                .parameter("language_hints", config.getLanguageHints().toArray(new String[0]))
                .fileUrls(Collections.singletonList(audioUrl))
                .build();

        try {
            Transcription transcription = new Transcription();

            // 提交转写请求
            TranscriptionResult result = transcription.asyncCall(param);
            System.out.println("语音识别请求已提交，RequestId: " + result.getRequestId());

            // 阻塞等待任务完成并获取结果
            TranscriptionQueryParam queryParam = TranscriptionQueryParam.FromTranscriptionParam(param, result.getTaskId());
            result = transcription.wait(queryParam);

            if (result.getTaskStatus() == TaskStatus.SUCCEEDED) {
                // 任务完成，解析结果
                return parseTranscriptionResult(result);
            } else {
                throw new RuntimeException("语音识别失败：未返回有效结果");
            }

        } catch (Exception e) {
            throw new Exception("语音识别失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析语音识别结果，转换为字幕格式
     *
     * @param result 识别结果
     * @return 格式化的字幕文本
     */
    private static String parseTranscriptionResult(TranscriptionResult result) {
        try {
            StringBuilder subtitleText = new StringBuilder();

            List<TranscriptionTaskResult> results = result.getResults();

            for (TranscriptionTaskResult transcriptionTaskResult : results) {
                String transcriptionUrl = transcriptionTaskResult.getTranscriptionUrl();

                if (transcriptionUrl != null && !transcriptionUrl.isEmpty()) {
                    // 获取transcriptionUrl的内容
                    String transcriptionContent = fetchTranscriptionContent(transcriptionUrl);

                    // 解析JSON内容
                    JsonObject transcriptionJson = JsonParser.parseString(transcriptionContent).getAsJsonObject();

                    // 解析转写结果
                    String srtContent = parseTranscriptionJson(transcriptionJson);
                    subtitleText.append(srtContent);
                }
            }

            if (subtitleText.isEmpty()) {
                subtitleText.append("1\n");
                subtitleText.append("00:00:00,000 --> 00:00:05,000\n");
                subtitleText.append("未能识别到语音内容\n");
            }

            return subtitleText.toString().trim();

        } catch (Exception e) {
            throw new RuntimeException("解析识别结果失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取transcriptionUrl的内容
     *
     * @param transcriptionUrl 转写结果URL
     * @return URL内容的JSON字符串
     */
    private static String fetchTranscriptionContent(String transcriptionUrl) throws Exception {
        URL url = new URL(transcriptionUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000); // 30秒连接超时
        connection.setReadTimeout(60000); // 60秒读取超时

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("获取转写结果失败，HTTP状态码: " + responseCode);
        }

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }

        return content.toString();
    }

    /**
     * 解析转写JSON结果，生成SRT格式字幕
     *
     * @param transcriptionJson 转写结果JSON对象
     * @return SRT格式字幕文本
     */
    private static String parseTranscriptionJson(JsonObject transcriptionJson) {
        StringBuilder srtContent = new StringBuilder();

        if (!transcriptionJson.has("transcripts")) {
            return "";
        }

        JsonArray transcripts = transcriptionJson.getAsJsonArray("transcripts");
        int sentenceNumber = 1; // 全局句子编号

        for (JsonElement transcriptElement : transcripts) {
            JsonObject transcript = transcriptElement.getAsJsonObject();

            if (transcript.has("sentences")) {
                JsonArray sentences = transcript.getAsJsonArray("sentences");

                for (JsonElement sentenceElement : sentences) {
                    JsonObject sentence = sentenceElement.getAsJsonObject();

                    // 获取句子信息
                    String text = sentence.has("text") ? sentence.get("text").getAsString().trim() : "";
                    long beginTime = sentence.has("begin_time") ? sentence.get("begin_time").getAsLong() : 0;
                    long endTime = sentence.has("end_time") ? sentence.get("end_time").getAsLong() : beginTime + 5000;

                    if (!text.isEmpty()) {
                        // 生成SRT条目
                        srtContent.append(sentenceNumber++).append("\n");
                        srtContent.append(formatTimestamp(beginTime))
                                .append(" --> ")
                                .append(formatTimestamp(endTime))
                                .append("\n");
                        srtContent.append(text).append("\n\n");
                    }
                }
            }
        }

        return srtContent.toString();
    }

    /**
     * 将毫秒时间戳格式化为SRT时间格式
     *
     * @param milliseconds 毫秒时间戳
     * @return SRT格式时间字符串 (HH:MM:SS,mmm)
     */
    private static String formatTimestamp(long milliseconds) {
        long hours = milliseconds / 3600000;
        long minutes = (milliseconds % 3600000) / 60000;
        long seconds = (milliseconds % 60000) / 1000;
        long millis = milliseconds % 1000;

        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis);
    }


    /**
     * 验证API Key是否已配置
     *
     * @return 如果API Key已配置返回true
     */
    public static boolean isApiKeyConfigured() {
        String apiKey = config.getDashScopeApiKey();
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
