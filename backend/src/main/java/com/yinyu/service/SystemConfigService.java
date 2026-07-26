package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yinyu.entity.SystemConfig;
import com.yinyu.mapper.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置（system_config 键值对）：读带 60 秒本地缓存，写后即时失效（api.md 18.9.1/18.9.2）
 */
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private static final long CACHE_TTL_MS = 60_000;

    private final SystemConfigMapper systemConfigMapper;

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private volatile long cacheLoadedAt = 0;

    private Map<String, String> all() {
        long now = System.currentTimeMillis();
        if (now - cacheLoadedAt > CACHE_TTL_MS) {
            synchronized (this) {
                if (now - cacheLoadedAt > CACHE_TTL_MS) {
                    cache.clear();
                    for (SystemConfig c : systemConfigMapper.selectList(null)) {
                        if (c.getConfigKey() != null && c.getConfigValue() != null) {
                            cache.put(c.getConfigKey(), c.getConfigValue());
                        }
                    }
                    cacheLoadedAt = now;
                }
            }
        }
        return cache;
    }

    public String get(String key, String defaultValue) {
        return all().getOrDefault(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(all().getOrDefault(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBool(String key, boolean defaultValue) {
        String v = all().get(key);
        if (v == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(v.trim()) || "1".equals(v.trim());
    }

    /** 设置查询：全部键值对 */
    public Map<String, String> settings() {
        return new LinkedHashMap<>(systemConfigMapper.selectList(null).stream()
                .collect(java.util.stream.Collectors.toMap(
                        SystemConfig::getConfigKey,
                        c -> c.getConfigValue() == null ? "" : c.getConfigValue(),
                        (a, b) -> a, LinkedHashMap::new)));
    }

    /** 设置修改：仅传需修改项，upsert */
    public void update(Map<String, Object> pairs) {
        if (pairs == null) {
            return;
        }
        for (Map.Entry<String, Object> e : pairs.entrySet()) {
            String key = e.getKey();
            String value = e.getValue() == null ? "" : String.valueOf(e.getValue());
            SystemConfig exists = systemConfigMapper.selectOne(
                    new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
            if (exists == null) {
                SystemConfig c = new SystemConfig();
                c.setConfigKey(key);
                c.setConfigValue(value);
                systemConfigMapper.insert(c);
            } else {
                exists.setConfigValue(value);
                systemConfigMapper.updateById(exists);
            }
        }
        evict();
    }

    public void evict() {
        cacheLoadedAt = 0;
    }
}
