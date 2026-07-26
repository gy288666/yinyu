package com.yinyu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MinIO 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    /** 逻辑桶名 -> 实际桶名：music/cover/avatar/banner */
    private Map<String, String> buckets;
    private int presignExpireSeconds = 1800;
    /** 降级静态路径前缀 */
    private String staticBase;
    /** 降级本地存储目录 */
    private String localStoreDir;
    /** 额外静态资源目录（只读，如仓库自带的 resource/static，含 music 等子目录），可为空 */
    private String localStaticExtra;

    public String bucket(String logical) {
        return buckets != null ? buckets.getOrDefault(logical, logical) : logical;
    }
}
