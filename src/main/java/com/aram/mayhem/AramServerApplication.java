package com.aram.mayhem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ARAM Mayhem Assistant 后端服务启动类
 *
 * 技术栈：Spring Boot 3.x + MyBatis-Plus + Redis + JWT
 * 模块：英雄管理、符文推荐、公告管理、社区攻略、用户认证
 */
@SpringBootApplication
public class AramServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AramServerApplication.class, args);
    }
}
