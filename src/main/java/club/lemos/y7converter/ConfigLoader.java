package club.lemos.y7converter;

import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.Map;
import java.util.List;

/**
 * 配置文件加载器
 * 负责加载和解析 YAML 配置文件
 */
public class ConfigLoader {
    
    private static ConfigLoader instance;
    private Map<String, Object> config;
    
    private ConfigLoader() {
        loadConfig();
    }
    
    /**
     * 获取配置加载器实例（单例模式）
     * 
     * @return 配置加载器实例
     */
    public static synchronized ConfigLoader getInstance() {
        if (instance == null) {
            instance = new ConfigLoader();
        }
        return instance;
    }
    
    /**
     * 加载配置文件
     */
    private void loadConfig() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.yaml")) {
            if (inputStream == null) {
                throw new RuntimeException("配置文件 config.yaml 未找到");
            }
            
            Yaml yaml = new Yaml();
            config = yaml.load(inputStream);
            
        } catch (Exception e) {
            throw new RuntimeException("加载配置文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取配置值
     * 
     * @param path 配置路径，使用点号分隔，如 "file_upload.upload_url"
     * @param defaultValue 默认值
     * @return 配置值
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfig(String path, T defaultValue) {
        try {
            String[] keys = path.split("\\.");
            Object current = config;
            
            for (String key : keys) {
                if (current instanceof Map) {
                    current = ((Map<String, Object>) current).get(key);
                } else {
                    return defaultValue;
                }
            }
            
            return current != null ? (T) current : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * 获取字符串配置值
     * 
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 字符串配置值
     */
    public String getString(String path, String defaultValue) {
        return getConfig(path, defaultValue);
    }
    
    /**
     * 获取整数配置值
     * 
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 整数配置值
     */
    public Integer getInt(String path, Integer defaultValue) {
        return getConfig(path, defaultValue);
    }
    
    /**
     * 获取布尔配置值
     * 
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 布尔配置值
     */
    public Boolean getBoolean(String path, Boolean defaultValue) {
        return getConfig(path, defaultValue);
    }
    
    /**
     * 获取列表配置值
     * 
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 列表配置值
     */
    public List<String> getStringList(String path, List<String> defaultValue) {
        return getConfig(path, defaultValue);
    }
    
    // 文件上传相关配置的便捷方法（阿里云OSS）
    
    /**
     * 获取 OSS Access Key ID
     * 
     * @return Access Key ID
     */
    public String getOssAccessKeyId() {
        return getString("file_upload.access_key_id", "");
    }
    
    /**
     * 获取 OSS Access Key Secret
     * 
     * @return Access Key Secret
     */
    public String getOssAccessKeySecret() {
        return getString("file_upload.access_key_secret", "");
    }
    
    /**
     * 获取 OSS Endpoint
     * 
     * @return OSS Endpoint
     */
    public String getOssEndpoint() {
        return getString("file_upload.endpoint", "https://oss-cn-hangzhou.aliyuncs.com");
    }
    
    /**
     * 获取 OSS Bucket 名称
     * 
     * @return Bucket 名称
     */
    public String getOssBucketName() {
        return getString("file_upload.bucket_name", "");
    }
    
    /**
     * 获取 OSS 对象存储前缀
     * 
     * @return 对象存储前缀
     */
    public String getOssObjectKeyPrefix() {
        return getString("file_upload.object_key_prefix", "uploads/");
    }
    
    /**
     * 是否使用 HTTPS
     * 
     * @return 是否使用 HTTPS
     */
    public boolean isOssUseHttps() {
        return getBoolean("file_upload.use_https", true);
    }
    
    /**
     * 获取 OSS 连接超时时间（毫秒）
     * 
     * @return 连接超时时间
     */
    public int getOssConnectionTimeout() {
        return getInt("file_upload.connection_timeout", 30000);
    }
    
    /**
     * 获取 OSS Socket 超时时间（毫秒）
     * 
     * @return Socket 超时时间
     */
    public int getOssSocketTimeout() {
        return getInt("file_upload.socket_timeout", 60000);
    }
    
    /**
     * 获取 OSS 最大连接数
     * 
     * @return 最大连接数
     */
    public int getOssMaxConnections() {
        return getInt("file_upload.max_connections", 100);
    }
    
    // 语音识别相关配置的便捷方法
    
    /**
     * 获取DashScope API Key
     * 先尝试从配置文件读取，如果为空则从环境变量读取
     * 
     * @return API Key
     */
    public String getDashScopeApiKey() {
        String apiKey = getString("speech_recognition.dashscope.api_key", "");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = System.getenv("DASHSCOPE_API_KEY");
        }
        return apiKey;
    }
    
    /**
     * 获取语音识别模型名称
     * 
     * @return 模型名称
     */
    public String getSpeechModel() {
        return getString("speech_recognition.dashscope.model", "paraformer-v2");
    }
    
    /**
     * 获取语言提示列表
     * 
     * @return 语言提示列表
     */
    public List<String> getLanguageHints() {
        return getStringList("speech_recognition.dashscope.language_hints", 
                            List.of("zh", "ja", "en"));
    }
    
    /**
     * 是否启用调试模式
     * 
     * @return 是否启用调试模式
     */
    public boolean isDebugMode() {
        return getBoolean("application.debug_mode", false);
    }
    
    // 字幕翻译相关配置的便捷方法
    
    /**
     * 获取翻译API Key
     * 先尝试从配置文件读取，如果为空则从环境变量读取
     * 
     * @return 翻译API Key
     */
    public String getTranslationApiKey() {
        String apiKey = getString("translation.dashscope.api_key", "");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = System.getenv("DASHSCOPE_API_KEY");
        }
        return apiKey;
    }
    
    /**
     * 获取翻译模型名称
     * 
     * @return 翻译模型名称
     */
    public String getTranslationModel() {
        return getString("translation.dashscope.model", "qwen-mt-plus");
    }
    
    /**
     * 获取翻译批处理大小
     * 
     * @return 批处理大小
     */
    public int getTranslationBatchSize() {
        return getInt("translation.dashscope.batch_size", 10);
    }
    
    /**
     * 获取翻译API调用间隔（毫秒）
     * 
     * @return API调用间隔
     */
    public int getTranslationApiDelay() {
        return getInt("translation.dashscope.api_delay", 1000);
    }
    
    /**
     * 获取默认源语言
     * 
     * @return 默认源语言
     */
    public String getDefaultSourceLanguage() {
        return getString("translation.default_languages.source_lang", "Chinese");
    }
    
    /**
     * 获取默认目标语言
     * 
     * @return 默认目标语言
     */
    public String getDefaultTargetLanguage() {
        return getString("translation.default_languages.target_lang", "English");
    }
    
    /**
     * 获取支持的语言列表
     * 
     * @return 支持的语言列表
     */
    public List<Map<String, String>> getSupportedLanguages() {
        try {
            return getConfig("translation.supported_languages", List.of());
        } catch (Exception e) {
            return List.of();
        }
    }
}
