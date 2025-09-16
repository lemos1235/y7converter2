package club.lemos.y7converter;

/**
 * 命令行操作类型常量
 */
public class CommandActions {

    /**
     * 音频提取操作（内部使用，字幕生成需要）
     */
    public static final int EXTRACT_AUDIO = 1;
    
    /**
     * 生成字幕操作（音频提取 + 语音识别）
     */
    public static final int GENERATE_SUBTITLE = 2;
    
    /**
     * 翻译字幕操作
     */
    public static final int TRANSLATE_SUBTITLE = 3;
}
