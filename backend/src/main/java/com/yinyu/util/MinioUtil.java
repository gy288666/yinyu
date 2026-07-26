package com.yinyu.util;

import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 工具类：上传 / 删除 / 预签名 URL。
 * MinIO 未启动时优雅降级：
 *  - 上传落盘到本地 storage 目录；
 *  - URL 返回 static-base/{bucket}/{objectKey} 静态路径拼接，保证接口不 500。
 * 可用性探测结果缓存 30 秒，避免每次请求都等待连接超时。
 */
@Slf4j
@Component
public class MinioUtil {

    private final MinioProperties props;
    private MinioClient client;

    private volatile boolean available = false;
    private volatile long lastCheckAt = 0;
    private static final long CHECK_INTERVAL_MS = 30_000;

    public MinioUtil(MinioProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        try {
            this.client = MinioClient.builder()
                    .endpoint(props.getEndpoint())
                    .credentials(props.getAccessKey(), props.getSecretKey())
                    .build();
            // 缩短超时时间，MinIO 未启动时快速失败进入降级
            this.client.setTimeout(TimeUnit.SECONDS.toMillis(3),
                    TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(10));
        } catch (Exception e) {
            log.warn("MinIO 客户端初始化失败，进入降级模式: {}", e.getMessage());
        }
        checkAvailable(true);
        if (available) {
            ensureBuckets();
        }
    }

    /** 探测 MinIO 是否可用（30 秒缓存） */
    public boolean isAvailable() {
        checkAvailable(false);
        return available;
    }

    private synchronized void checkAvailable(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastCheckAt < CHECK_INTERVAL_MS) {
            return;
        }
        lastCheckAt = now;
        if (client == null) {
            available = false;
            return;
        }
        try {
            client.bucketExists(BucketExistsArgs.builder().bucket(props.bucket("music")).build());
            if (!available) {
                log.info("MinIO 可用: {}", props.getEndpoint());
                available = true;
                ensureBuckets();
            }
        } catch (Exception e) {
            if (available || lastCheckAt == now) {
                log.warn("MinIO 不可用，使用本地降级存储: {}", e.getMessage());
            }
            available = false;
        }
    }

    private void ensureBuckets() {
        if (props.getBuckets() == null) {
            return;
        }
        for (Map.Entry<String, String> e : props.getBuckets().entrySet()) {
            try {
                if (!client.bucketExists(BucketExistsArgs.builder().bucket(e.getValue()).build())) {
                    client.makeBucket(MakeBucketArgs.builder().bucket(e.getValue()).build());
                }
            } catch (Exception ex) {
                log.warn("创建桶 {} 失败: {}", e.getValue(), ex.getMessage());
            }
        }
    }

    /**
     * 上传文件。MinIO 不可用时写入本地 storage 目录。
     *
     * @param logicalBucket 逻辑桶名 music/cover/avatar/banner
     * @param objectKey     桶内对象路径
     */
    public void upload(String logicalBucket, String objectKey, MultipartFile file) {
        String bucket = props.bucket(logicalBucket);
        if (isAvailable()) {
            try (InputStream in = file.getInputStream()) {
                client.putObject(PutObjectArgs.builder()
                        .bucket(bucket).object(objectKey)
                        .stream(in, file.getSize(), -1)
                        .contentType(file.getContentType() == null
                                ? "application/octet-stream" : file.getContentType())
                        .build());
                return;
            } catch (Exception e) {
                log.warn("MinIO 上传失败，降级本地存储: {}", e.getMessage());
            }
        }
        // 降级：落盘本地
        try {
            Path target = Paths.get(props.getLocalStoreDir(), bucket, objectKey).toAbsolutePath();
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            throw new BizException(ErrorCode.FILE_INVALID, "文件保存失败: " + e.getMessage());
        }
    }

    /** 删除对象（降级模式删除本地文件），失败仅记日志 */
    public void delete(String logicalBucket, String objectKey) {
        String bucket = props.bucket(logicalBucket);
        if (isAvailable()) {
            try {
                client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
                return;
            } catch (Exception e) {
                log.warn("MinIO 删除失败: {}", e.getMessage());
            }
        }
        try {
            Files.deleteIfExists(Paths.get(props.getLocalStoreDir(), bucket, objectKey));
        } catch (Exception e) {
            log.warn("本地文件删除失败: {}", e.getMessage());
        }
    }

    /**
     * 生成访问 URL：可用时签发预签名 GET URL，不可用时返回静态路径拼接
     */
    public String presignedGetUrl(String logicalBucket, String objectKey) {
        return presignedGetUrl(logicalBucket, objectKey, props.getPresignExpireSeconds(), null);
    }

    /**
     * @param attachmentFilename 非空时以附件方式下载
     */
    public String presignedGetUrl(String logicalBucket, String objectKey,
                                  int expireSeconds, String attachmentFilename) {
        String bucket = props.bucket(logicalBucket);
        if (isAvailable()) {
            try {
                GetPresignedObjectUrlArgs.Builder builder = GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET).bucket(bucket).object(objectKey)
                        .expiry(expireSeconds, TimeUnit.SECONDS);
                if (attachmentFilename != null) {
                    builder.extraQueryParams(Map.of("response-content-disposition",
                            "attachment; filename=\"" + attachmentFilename + "\""));
                }
                return client.getPresignedObjectUrl(builder.build());
            } catch (Exception e) {
                log.warn("MinIO 预签名失败，返回降级路径: {}", e.getMessage());
            }
        }
        // 降级：静态路径拼接
        String base = props.getStaticBase();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + bucket + "/" + objectKey;
    }

    public int getPresignExpireSeconds() {
        return props.getPresignExpireSeconds();
    }

    /**
     * 图片等公开资源的展示 URL：绝对地址原样返回，相对路径拼接静态前缀。
     * 测试数据中的 image/xxx 相对路径统一走静态前缀，避免逐个签名。
     */
    public String publicImageUrl(String path) {
        if (path == null || path.isEmpty() || path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String base = props.getStaticBase();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + (path.startsWith("/") ? path.substring(1) : path);
    }
}
