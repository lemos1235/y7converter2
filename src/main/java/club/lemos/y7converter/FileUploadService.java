package club.lemos.y7converter;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 文件上传服务类
 * 使用阿里云OSS进行文件上传
 */
public class FileUploadService {

    private final ConfigLoader config;
    private OSS ossClient;

    public FileUploadService() {
        this.config = ConfigLoader.getInstance();
        // 初始化 OSS 客户端
        initOssClient();
    }

    /**
     * 初始化阿里云OSS客户端
     */
    private void initOssClient() {
        String accessKeyId = config.getOssAccessKeyId();
        String accessKeySecret = config.getOssAccessKeySecret();
        String endpoint = config.getOssEndpoint();

        if (accessKeyId.isEmpty() || accessKeySecret.isEmpty()) {
            throw new IllegalStateException("OSS Access Key ID 或 Access Key Secret 未配置");
        }

        if (endpoint.isEmpty()) {
            throw new IllegalStateException("OSS Endpoint 未配置");
        }

        // 创建ClientBuilderConfiguration配置类
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setConnectionTimeout(config.getOssConnectionTimeout());
        conf.setSocketTimeout(config.getOssSocketTimeout());
        conf.setMaxConnections(config.getOssMaxConnections());
        conf.setSupportCname(false);
        conf.setProtocol(config.isOssUseHttps() ?
                com.aliyun.oss.common.comm.Protocol.HTTPS :
                com.aliyun.oss.common.comm.Protocol.HTTP);

        // 创建OSSClient实例
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret, conf);

        if (config.isDebugMode()) {
            System.out.println("OSS 客户端初始化成功");
            System.out.println("Endpoint: " + endpoint);
            System.out.println("Bucket: " + config.getOssBucketName());
        }
    }

    /**
     * 上传文件到阿里云OSS
     *
     * @param file 要上传的文件
     * @return 上传结果对象
     * @throws IOException 如果上传失败
     */
    public UploadResult uploadFile(File file) throws IOException {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("文件不存在或不是有效文件: " +
                    (file != null ? file.getAbsolutePath() : "null"));
        }
        String bucketName = config.getOssBucketName();
        if (bucketName.isEmpty()) {
            throw new IllegalStateException("OSS Bucket 名称未配置");
        }

        // 生成对象键名
        String objectKey = generateObjectKey(file);

        if (config.isDebugMode()) {
            System.out.println("OSS上传文件: " + file.getAbsolutePath());
            System.out.println("Bucket: " + bucketName);
            System.out.println("Object Key: " + objectKey);
            System.out.println("文件大小: " + file.length() + " 字节");
        }

        try {
            // 创建上传请求
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectKey, file);

            // 执行上传
            PutObjectResult result = ossClient.putObject(putObjectRequest);

            if (config.isDebugMode()) {
                System.out.println("OSS上传成功");
                System.out.println("ETag: " + result.getETag());
                System.out.println("RequestId: " + result.getRequestId());
            }

            // 构造上传结果
            UploadResult uploadResult = new UploadResult();
            uploadResult.setStatus("success");
            uploadResult.setMessage("文件上传成功");
            uploadResult.setFilename(objectKey);
            uploadResult.setSize(file.length());
            uploadResult.setOriginalFile(file);

            // 构建访问URL
            String fileUrl = buildOssFileUrl(objectKey);
            uploadResult.setFileUrl(fileUrl);

            return uploadResult;

        } catch (Exception e) {
            throw new IOException("OSS文件上传失败: " + e.getMessage(), e);
        }
    }


    /**
     * 生成OSS对象键名
     *
     * @param file 文件对象
     * @return 对象键名
     */
    private String generateObjectKey(File file) {
        String prefix = config.getOssObjectKeyPrefix();
        if (!prefix.endsWith("/") && !prefix.isEmpty()) {
            prefix += "/";
        }

        // 生成时间戳目录结构：audio/2025/01
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM");
        String datePath = dateFormat.format(new Date());

        // 生成唯一文件名：UUID_原文件名
        String fileName = file.getName();
        String fileExtension = "";
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            fileExtension = fileName.substring(lastDotIndex);
            fileName = fileName.substring(0, lastDotIndex);
        }

        String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName + fileExtension;

        return prefix + datePath + "/" + uniqueFileName;
    }

    /**
     * 构建OSS文件访问URL
     *
     * @param objectKey 对象键名
     * @return 文件访问URL
     */
    private String buildOssFileUrl(String objectKey) {
        String endpoint = config.getOssEndpoint();
        String bucketName = config.getOssBucketName();

        // 移除endpoint中的https://或http://前缀
        String endpointHost = endpoint.replaceFirst("^https?://", "");

        // 构建URL：https://bucket.endpoint/objectKey
        String protocol = config.isOssUseHttps() ? "https" : "http";
        return String.format("%s://%s.%s/%s", protocol, bucketName, endpointHost, objectKey);
    }

    /**
     * 从OSS删除已上传的文件
     * 
     * @param objectKey OSS对象键名
     * @return 是否删除成功
     */
    public boolean deleteFileFromOSS(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            if (config.isDebugMode()) {
                System.out.println("OSS对象键名为空，无法删除");
            }
            return false;
        }
        
        String bucketName = config.getOssBucketName();
        if (bucketName.isEmpty()) {
            System.err.println("OSS Bucket 名称未配置，无法删除文件");
            return false;
        }
        
        try {
            if (config.isDebugMode()) {
                System.out.println("正在从OSS删除文件: " + objectKey);
                System.out.println("Bucket: " + bucketName);
            }
            
            // 删除OSS对象
            ossClient.deleteObject(bucketName, objectKey);
            
            if (config.isDebugMode()) {
                System.out.println("成功从OSS删除文件: " + objectKey);
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("从OSS删除文件时发生错误: " + e.getMessage());
            System.err.println("文件: " + objectKey);
            return false;
        }
    }

    /**
     * 从上传结果中删除OSS文件
     * 
     * @param uploadResult 上传结果对象
     * @return 是否删除成功
     */
    public boolean deleteUploadedFile(UploadResult uploadResult) {
        if (uploadResult == null || !uploadResult.isSuccess()) {
            if (config.isDebugMode()) {
                System.out.println("上传结果无效，无法删除OSS文件");
            }
            return false;
        }
        
        return deleteFileFromOSS(uploadResult.getFilename());
    }

    /**
     * 关闭文件上传服务，释放资源
     * 主要用于关闭OSS客户端连接
     */
    public void close() {
        if (ossClient != null) {
            try {
                ossClient.shutdown();
                if (config.isDebugMode()) {
                    System.out.println("OSS 客户端已关闭");
                }
            } catch (Exception e) {
                System.err.println("关闭OSS客户端时发生错误: " + e.getMessage());
            }
        }
    }

    /**
     * 上传结果类
     */
    public static class UploadResult {
        private String status;
        private String message;
        private String filename;
        private Long size;
        private String fileUrl;
        private File originalFile;

        public boolean isSuccess() {
            return "success".equalsIgnoreCase(status);
        }

        // Getters and Setters
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
            this.size = size;
        }

        public String getFileUrl() {
            return fileUrl;
        }

        public void setFileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
        }

        public File getOriginalFile() {
            return originalFile;
        }

        public void setOriginalFile(File originalFile) {
            this.originalFile = originalFile;
        }

        @Override
        public String toString() {
            return String.format("UploadResult{status='%s', message='%s', filename='%s', size=%d, fileUrl='%s'}",
                    status, message, filename, size, fileUrl);
        }
    }
}
