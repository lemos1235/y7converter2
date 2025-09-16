package club.lemos.y7converter;

import java.io.File;

/**
 * 文件名生成工具类
 * 负责处理各种文件名生成和语言代码映射的功能
 */
public class FileNameUtils {

    private static final ConfigLoader config = ConfigLoader.getInstance();

    /**
     * 根据输入文件和目标语言生成翻译后的文件名
     *
     * @param inputFile  输入文件
     * @param targetLang 目标语言
     * @return 建议的输出文件名
     */
    public static String generateTranslatedFileName(File inputFile, String targetLang) {
        if (inputFile == null) {
            return "translated_subtitle.srt";
        }
        
        String baseName = getBaseName(inputFile);
        String langCode = getLanguageCode(targetLang);
        
        if (langCode != null) {
            return baseName + "." + langCode + ".srt";
        } else {
            return baseName + "_translated.srt";
        }
    }

    /**
     * 根据当前配置生成默认的翻译文件名
     *
     * @param inputFile 输入文件
     * @return 建议的输出文件名
     */
    public static String generateDefaultTranslatedFileName(File inputFile) {
        String targetLang = config.getDefaultTargetLanguage();
        return generateTranslatedFileName(inputFile, targetLang);
    }

    /**
     * 生成字幕文件的默认文件名
     *
     * @param inputFile 输入文件
     * @return 字幕文件名
     */
    public static String generateSubtitleFileName(File inputFile) {
        if (inputFile == null) {
            return "subtitle.srt";
        }
        
        String baseName = getBaseName(inputFile);
        return baseName + ".srt";
    }

    /**
     * 根据语言名称获取对应的语言代码
     *
     * @param languageName 语言名称
     * @return 语言代码，如果不支持则返回null
     */
    public static String getLanguageCode(String languageName) {
        if (languageName == null) return null;
        
        switch (languageName.toLowerCase()) {
            case "chinese":
            case "中文":
                return "zh";
            case "english":
            case "英文":
                return "en";
            case "japanese":
            case "日文":
                return "ja";
            case "korean":
            case "韩文":
                return "ko";
            case "french":
            case "法文":
                return "fr";
            case "german":
            case "德文":
                return "de";
            case "spanish":
            case "西班牙文":
                return "es";
            case "russian":
            case "俄文":
                return "ru";
            case "portuguese":
            case "葡萄牙文":
                return "pt";
            case "italian":
            case "意大利文":
                return "it";
            case "dutch":
            case "荷兰文":
                return "nl";
            case "arabic":
            case "阿拉伯文":
                return "ar";
            case "thai":
            case "泰文":
                return "th";
            case "vietnamese":
            case "越南文":
                return "vi";
            default:
                return null;
        }
    }

    /**
     * 根据语言代码获取语言名称
     *
     * @param languageCode 语言代码
     * @return 语言名称，如果不支持则返回null
     */
    public static String getLanguageName(String languageCode) {
        if (languageCode == null) return null;
        
        switch (languageCode.toLowerCase()) {
            case "zh":
                return "中文";
            case "en":
                return "英文";
            case "ja":
                return "日文";
            case "ko":
                return "韩文";
            case "fr":
                return "法文";
            case "de":
                return "德文";
            case "es":
                return "西班牙文";
            case "ru":
                return "俄文";
            case "pt":
                return "葡萄牙文";
            case "it":
                return "意大利文";
            case "nl":
                return "荷兰文";
            case "ar":
                return "阿拉伯文";
            case "th":
                return "泰文";
            case "vi":
                return "越南文";
            default:
                return null;
        }
    }

    /**
     * 获取文件的基础名称（不包含扩展名）
     *
     * @param file 文件
     * @return 基础名称
     */
    public static String getBaseName(File file) {
        if (file == null) return "";
        return file.getName().replaceFirst("[.][^.]+$", "");
    }

    /**
     * 获取文件的扩展名
     *
     * @param file 文件
     * @return 扩展名（包含点号），如果没有扩展名则返回空字符串
     */
    public static String getFileExtension(File file) {
        if (file == null) return "";
        
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex);
        }
        
        return "";
    }

    /**
     * 检查文件是否为字幕文件
     *
     * @param file 文件
     * @return 如果是字幕文件返回true
     */
    public static boolean isSubtitleFile(File file) {
        if (file == null) return false;
        
        String extension = getFileExtension(file).toLowerCase();
        return extension.equals(".srt") || extension.equals(".vtt") || 
               extension.equals(".ass") || extension.equals(".ssa");
    }

    /**
     * 检查文件是否为视频文件
     *
     * @param file 文件
     * @return 如果是视频文件返回true
     */
    public static boolean isVideoFile(File file) {
        if (file == null) return false;
        
        String extension = getFileExtension(file).toLowerCase();
        return extension.equals(".mp4") || extension.equals(".avi") || 
               extension.equals(".mkv") || extension.equals(".mov") || 
               extension.equals(".wmv") || extension.equals(".flv") ||
               extension.equals(".webm") || extension.equals(".m4v");
    }

    /**
     * 检查文件是否为音频文件
     *
     * @param file 文件
     * @return 如果是音频文件返回true
     */
    public static boolean isAudioFile(File file) {
        if (file == null) return false;
        
        String extension = getFileExtension(file).toLowerCase();
        return extension.equals(".mp3") || extension.equals(".wav") || 
               extension.equals(".aac") || extension.equals(".flac") || 
               extension.equals(".ogg") || extension.equals(".m4a") ||
               extension.equals(".wma");
    }
}
