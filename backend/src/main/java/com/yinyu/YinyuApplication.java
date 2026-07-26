package com.yinyu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 音域 YINYU 音乐平台后端启动类
 */
@EnableScheduling
@SpringBootApplication
public class YinyuApplication {

    public static void main(String[] args) {
        SpringApplication.run(YinyuApplication.class, args);
    }
}
