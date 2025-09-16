package club.lemos;

import club.lemos.y7converter.CommandActions;
import club.lemos.y7converter.CommandExecutor;
import club.lemos.y7converter.CommandResult;
import club.lemos.y7converter.FFmpegUtil;
import club.lemos.y7converter.FileNameUtils;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * 主界面
 */
public class App {

    // UI组件
    private final JFrame frame = new JFrame("视频字幕生成工具");
    private final JPanel panel = new JPanel();

    private final JButton fileSelectorBtn = new JButton("选择文件");
    private final JButton generateSubtitleBtn = new JButton("生成字幕");
    private final JButton translateSubtitleBtn = new JButton("翻译字幕");
    private final JButton cancelBtn = new JButton("取消");
    private final JButton downloadBtn = new JButton("保存字幕");
    private final JButton nextOneBtn = new JButton("下一个");
    private final JButton retryBtn = new JButton("重试");

    // 数据属性
    private File selectedFile;
    private File destFile;
    private Integer commandAction;
    private CommandResult commandResult;

    // 界面面板
    private JPanel fileSelectPanel;
    private JPanel actionsPanel;
    private JPanel handlingPanel;
    private JPanel errorPanel;
    private JPanel donePanel;

    /**
     * 构造函数，初始化界面
     */
    public App() {
        initializePanels();
        setupEventListeners();
        setupFrame();
        setupShutdownHook();
    }

    /**
     * 应用程序入口点
     * 设置系统属性，初始化界面外观，并启动应用程序
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                LookAndFeel lookAndFeel = new FlatMacLightLaf();
                UIManager.setLookAndFeel(lookAndFeel);
            } catch (UnsupportedLookAndFeelException e) {
                throw new RuntimeException(e);
            }
            new App();
        });
    }

    /**
     * 初始化主窗口
     */
    private void setupFrame() {
        frame.add(panel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(320, 240));
        frame.setPreferredSize(new Dimension(320, 240));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * 初始化各个面板
     */
    private void initializePanels() {
        panel.setLayout(new MigLayout("al center center"));

        fileSelectPanel = new JPanel(new MigLayout());
        fileSelectorBtn.setFocusable(false);
        fileSelectPanel.add(fileSelectorBtn);
        panel.add(fileSelectPanel);

        actionsPanel = new JPanel(new MigLayout("al center center"));
        actionsPanel.add(generateSubtitleBtn, "al center, wrap");
        actionsPanel.add(translateSubtitleBtn, "al center, wrap");
        cancelBtn.setForeground(Color.GRAY);
        actionsPanel.add(cancelBtn, "al center");

        handlingPanel = new JPanel(new MigLayout("al center center"));
        JLabel handlingLabel = new JLabel("正在处理~");
        handlingLabel.setFont(handlingLabel.getFont().deriveFont(Font.PLAIN, 12f));
        handlingPanel.add(handlingLabel, "al center, wrap");

        errorPanel = new JPanel(new MigLayout("al center center"));
        JLabel errorLabel = new JLabel("出错了~");
        errorLabel.setFont(errorLabel.getFont().deriveFont(Font.PLAIN, 12f));
        errorPanel.add(errorLabel, "al center, wrap");
        errorPanel.add(retryBtn, "al center");

        donePanel = new JPanel(new MigLayout("al center center"));

        FileTransferHandler transferHandler = new FileTransferHandler();
        panel.setTransferHandler(transferHandler);
    }

    /**
     * 初始化事件监听器
     */
    private void setupEventListeners() {
        fileSelectorBtn.addActionListener(e -> {
            FileDialog fileDialog = new FileDialog(frame, "选择文件", FileDialog.LOAD);
            fileDialog.setVisible(true);

            String directory = fileDialog.getDirectory();
            String file = fileDialog.getFile();

            if (file != null) {
                File selectedFileFromDialog = new File(directory, file);
                handleFileSelection(selectedFileFromDialog);
            }
        });

        ActionListener commandActionListener = e -> {
            try {
                panel.remove(actionsPanel);
                panel.add(handlingPanel);
                panel.revalidate();
                panel.repaint();

                SwingWorker<CommandResult, Void> worker = new SwingWorker<>() {
                    @Override
                    protected CommandResult doInBackground() throws Exception {
                        String suffix = getOutputFileSuffix(commandAction, selectedFile);
                        File tmpDestFile = Files.createTempFile("processed", suffix).toFile();
                        return CommandExecutor.execute(commandAction, selectedFile, tmpDestFile);
                    }

                    @Override
                    protected void done() {
                        try {
                            commandResult = get();
                            destFile = commandResult.getResultFile();
                            panel.remove(handlingPanel);
                            createDonePanelWithExport();
                            panel.add(donePanel);
                            panel.revalidate();
                            panel.repaint();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            panel.remove(handlingPanel);
                            panel.add(errorPanel);
                            panel.revalidate();
                            panel.repaint();
                        }
                    }
                };

                worker.execute();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };

        generateSubtitleBtn.addActionListener(e -> {
            commandAction = CommandActions.GENERATE_SUBTITLE;
            commandActionListener.actionPerformed(e);
        });

        translateSubtitleBtn.addActionListener(e -> {
            commandAction = CommandActions.TRANSLATE_SUBTITLE;
            commandActionListener.actionPerformed(e);
        });

        downloadBtn.addActionListener(e -> {
            FileDialog fileDialog = new FileDialog(frame, "保存字幕文件", FileDialog.SAVE);

            // 设置默认字幕文件名
            String defaultName = generateDefaultFileName();
            fileDialog.setFile(defaultName);

            fileDialog.setVisible(true);

            String directory = fileDialog.getDirectory();
            String file = fileDialog.getFile();

            if (file != null) {
                File saveFile = new File(directory, file);
                try {
                    FileUtils.copyFile(destFile, saveFile);
                    destFile = saveFile;

                    // 显示导出成功对话框
                    JOptionPane.showMessageDialog(frame,
                            "字幕文件保存成功！",
                            "保存成功",
                            JOptionPane.PLAIN_MESSAGE);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        nextOneBtn.addActionListener(e -> returnToFileSelection());
        cancelBtn.addActionListener(e -> returnToFileSelection());
        retryBtn.addActionListener(e -> returnToFileSelection());
    }

    /**
     * 创建完成面板并添加导出功能
     * 显示操作完成信息和耗时，并提供导出和下一个文件的按钮
     */
    private void createDonePanelWithExport() {
        donePanel.removeAll();

        if (commandResult != null) {
            String timeInfo = String.format("%s完成，耗时：%s",
                    commandResult.getActionDescription(),
                    commandResult.getFormattedTime());
            JLabel timeLabel = new JLabel(timeInfo);
            timeLabel.setFont(timeLabel.getFont().deriveFont(Font.PLAIN, 13f));
            timeLabel.setForeground(Color.GRAY);
            donePanel.add(timeLabel, "span, align center, wrap 20");
        }

        donePanel.add(nextOneBtn, "split 2, align center");
        donePanel.add(downloadBtn, "align center");

        donePanel.revalidate();
        donePanel.repaint();
    }

    /**
     * 返回文件选择阶段
     */
    private void returnToFileSelection() {
        selectedFile = null;
        destFile = null;
        commandResult = null;
        Component currentComponent = getCurrentDisplayedComponent();
        if (currentComponent != null) {
            panel.remove(currentComponent);
        }
        panel.add(fileSelectPanel);
        panel.revalidate();
        panel.repaint();
    }
    
    /**
     * 处理文件选择事件
     *
     * @param file 用户选择的文件
     */
    private void handleFileSelection(File file) {
        selectedFile = file;

        // 根据文件类型显示不同的操作选项
        updateActionPanel();

        Component currentComponent = getCurrentDisplayedComponent();
        if (currentComponent == fileSelectPanel) {
            panel.remove(fileSelectPanel);
            panel.add(actionsPanel);
            panel.revalidate();
            panel.repaint();
        }
    }

    /**
     * 根据选择的文件类型更新操作面板
     */
    private void updateActionPanel() {
        // 清空现有组件
        actionsPanel.removeAll();

        if (selectedFile != null) {
            if (FileNameUtils.isSubtitleFile(selectedFile)) {
                // 字幕文件：只显示翻译选项
                actionsPanel.add(translateSubtitleBtn, "al center, wrap");
                fileSelectorBtn.setText("选择字幕文件");
            } else if (FileNameUtils.isVideoFile(selectedFile) || FileNameUtils.isAudioFile(selectedFile)) {
                // 视频/音频文件：显示生成字幕选项
                actionsPanel.add(generateSubtitleBtn, "al center, wrap");
                fileSelectorBtn.setText("选择视频文件");
            } else {
                // 未知文件类型：显示所有选项
                actionsPanel.add(generateSubtitleBtn, "al center, wrap");
                actionsPanel.add(translateSubtitleBtn, "al center, wrap");
                fileSelectorBtn.setText("选择文件");
            }
        } else {
            // 默认显示所有选项
            actionsPanel.add(generateSubtitleBtn, "al center, wrap");
            actionsPanel.add(translateSubtitleBtn, "al center, wrap");
            fileSelectorBtn.setText("选择文件");
        }

        // 添加取消按钮
        cancelBtn.setForeground(Color.GRAY);
        actionsPanel.add(cancelBtn, "al center");

        actionsPanel.revalidate();
        actionsPanel.repaint();
    }

    /**
     * 获取当前显示的面板组件
     *
     * @return 当前显示的面板组件，如果没有组件则返回null
     */
    private Component getCurrentDisplayedComponent() {
        if (panel.getComponentCount() > 0) {
            return panel.getComponent(0);
        }
        return null;
    }

    /**
     * 根据操作类型和源文件获取输出文件后缀
     * 
     * @param action 操作类型
     * @param sourceFile 源文件
     * @return 输出文件后缀
     */
    private String getOutputFileSuffix(int action, File sourceFile) {
        return switch (action) {
            case CommandActions.EXTRACT_AUDIO -> "_extracted.aac"; // 音频提取后缀
            case CommandActions.GENERATE_SUBTITLE -> "_subtitle.srt"; // 字幕文件后缀
            case CommandActions.TRANSLATE_SUBTITLE -> "_translated.srt"; // 翻译后字幕文件后缀
            default -> ".out";
        };
    }

    /**
     * 生成默认保存文件名
     * 
     * @return 默认文件名
     */
    private String generateDefaultFileName() {
        if (selectedFile == null) {
            return "subtitle.srt";
        }
        
        // 如果是翻译操作，使用 FileNameUtils 的方法
        if (commandAction != null && commandAction == CommandActions.TRANSLATE_SUBTITLE) {
            return FileNameUtils.generateDefaultTranslatedFileName(selectedFile);
        }
        
        // 默认情况（生成字幕等其他操作）
        return FileNameUtils.generateSubtitleFileName(selectedFile);
    }

    /**
     * 设置关闭钩子来清理FFmpeg临时文件
     */
    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(FFmpegUtil::cleanup));
    }

    /**
     * 文件拖拽处理器
     * 处理文件拖拽到应用程序的功能，支持文件导入和视觉反馈
     */
    private class FileTransferHandler extends TransferHandler {
        private Color originalPanelColor;
        private boolean isDragInProgress = false;
        private Timer resetTimer;

        /**
         * 检查是否可以导入拖拽的文件
         *
         * @param support 拖拽支持对象
         * @return 如果可以导入文件则返回true，否则返回false
         */
        @Override
        public boolean canImport(TransferSupport support) {
            Component currentComponent = getCurrentDisplayedComponent();
            if (currentComponent != fileSelectPanel) {
                scheduleAppearanceReset();
                return false;
            }

            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                scheduleAppearanceReset();
                return false;
            }

            boolean copySupported = (COPY & support.getSourceDropActions()) == COPY;
            if (copySupported) {
                support.setDropAction(COPY);
                showDragFeedback();
                return true;
            } else {
                scheduleAppearanceReset();
            }

            return false;
        }

        /**
         * 导入拖拽的文件数据
         *
         * @param support 拖拽支持对象
         * @return 如果成功导入文件则返回true，否则返回false
         */
        @Override
        public boolean importData(TransferSupport support) {
            try {
                Component currentComponent = getCurrentDisplayedComponent();
                if (currentComponent != fileSelectPanel) {
                    return false;
                }

                if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    return false;
                }

                Transferable transferable = support.getTransferable();
                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

                if (!files.isEmpty()) {
                    File droppedFile = files.get(0);
                    if (droppedFile.isFile()) {
                        SwingUtilities.invokeLater(() -> handleFileSelection(droppedFile));
                        return true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                restoreOriginalAppearance();
            }

            return false;
        }

        /**
         * 显示拖拽反馈效果
         * 改变面板背景色和边框以提供视觉反馈
         */
        private void showDragFeedback() {
            if (!isDragInProgress) {
                originalPanelColor = panel.getBackground();
                panel.setBackground(new Color(230, 247, 255));
                panel.setBorder(BorderFactory.createDashedBorder(Color.BLUE, 2, 5, 5, true));
                panel.repaint();
                isDragInProgress = true;
            }

            if (resetTimer != null && resetTimer.isRunning()) {
                resetTimer.stop();
            }
        }

        /**
         * 安排外观重置
         * 使用定时器延迟重置面板外观
         */
        private void scheduleAppearanceReset() {
            if (isDragInProgress) {
                if (resetTimer != null && resetTimer.isRunning()) {
                    resetTimer.stop();
                }

                resetTimer = new Timer(100, e -> restoreOriginalAppearance());
                resetTimer.setRepeats(false);
                resetTimer.start();
            }
        }

        /**
         * 恢复原始外观
         * 重置面板背景色和边框到原始状态
         */
        private void restoreOriginalAppearance() {
            if (isDragInProgress) {
                panel.setBackground(originalPanelColor);
                panel.setBorder(null);
                panel.repaint();
                isDragInProgress = false;
            }

            if (resetTimer != null && resetTimer.isRunning()) {
                resetTimer.stop();
            }
        }
    }
}
