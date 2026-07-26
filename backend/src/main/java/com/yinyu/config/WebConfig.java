package com.yinyu.config;

import com.yinyu.security.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web 配置：鉴权拦截器、CORS、降级静态资源映射
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final MinioProperties minioProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // MinIO 降级时的本地文件访问路径 /static/{bucket}/{objectKey}
        String dir = Paths.get(minioProperties.getLocalStoreDir()).toAbsolutePath().toString();
        var handler = registry.addResourceHandler("/static/**").addResourceLocations("file:" + dir + "/");
        // 仓库自带的只读素材目录（如 resource/static/music），作为第二查找位置
        String extra = minioProperties.getLocalStaticExtra();
        if (extra != null && !extra.isBlank()) {
            handler.addResourceLocations("file:" + Paths.get(extra).toAbsolutePath().normalize() + "/");
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
