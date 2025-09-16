package club.lemos.y7converter;

import java.io.File;

/**
 * 命令行操作结果类
 */
public class CommandResult {

    private final File resultFile;
    private final long processingTimeMs;
    private final int action;
    private final String output;
    private final String error;
    private final int exitCode;

    /**
     * 构造函数
     *
     * @param resultFile       结果文件
     * @param processingTimeMs 处理时间（毫秒）
     * @param action           操作类型
     * @param output           命令输出
     * @param error            错误输出
     * @param exitCode         退出码
     */
    public CommandResult(File resultFile, long processingTimeMs, int action, String output, String error, int exitCode) {
        this.resultFile = resultFile;
        this.processingTimeMs = processingTimeMs;
        this.action = action;
        this.output = output;
        this.error = error;
        this.exitCode = exitCode;
    }

    /**
     * 获取结果文件
     */
    public File getResultFile() {
        return resultFile;
    }

    /**
     * 获取处理时间（毫秒）
     */
    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    /**
     * 获取处理时间（秒）
     */
    public double getProcessingTimeSeconds() {
        return processingTimeMs / 1000.0;
    }

    /**
     * 获取操作类型
     */
    public int getAction() {
        return action;
    }

    /**
     * 获取命令输出
     */
    public String getOutput() {
        return output;
    }

    /**
     * 获取错误输出
     */
    public String getError() {
        return error;
    }

    /**
     * 获取退出码
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * 获取操作类型描述
     */
    public String getActionDescription() {
        switch (action) {
            case CommandActions.EXTRACT_AUDIO:
                return "音频提取";
            case CommandActions.GENERATE_SUBTITLE:
                return "字幕生成";
            default:
                return "处理";
        }
    }

    /**
     * 获取格式化的时间描述
     */
    public String getFormattedTime() {
        if (processingTimeMs < 1000) {
            return processingTimeMs + " 毫秒";
        } else {
            return String.format("%.2f 秒", getProcessingTimeSeconds());
        }
    }

    /**
     * 判断命令是否执行成功
     */
    public boolean isSuccess() {
        return exitCode == 0;
    }
}
