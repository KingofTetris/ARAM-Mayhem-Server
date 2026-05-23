package com.aram.mayhem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ARAM Mayhem Assistant 后端服务启动类 —— Spring Boot 应用的入口点
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这是整个后端服务的启动入口，包含 main 方法。
 * 当你运行这个类时，Spring Boot 会自动完成以下工作：
 * 1. 启动内嵌的 Tomcat 服务器（默认端口 8080）
 * 2. 扫描 com.aram.mayhem 包下的所有组件（@Controller、@Service、@Repository 等）
 * 3. 自动配置数据源、Redis、MyBatis-Plus 等
 * 4. 初始化 Spring Security 安全框架
 * 5. 执行 DataInitializer 等数据初始化器
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、@SpringBootApplication 注解说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * @SpringBootApplication 是一个组合注解，等同于以下三个注解：
 * - @SpringBootConfiguration：标记这是一个配置类
 * - @EnableAutoConfiguration：启用 Spring Boot 自动配置
 * - @ComponentScan：自动扫描当前包及子包下的组件
 *
 * 扫描范围：com.aram.mayhem 及其所有子包
 * 包括：controller、service、mapper、dto、entity、config、security 等
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、技术栈概览
 * ═══════════════════════════════════════════════════════════════════
 *
 * 核心框架：Spring Boot 3.x
 * ORM 框架：MyBatis-Plus（基于 MyBatis 的增强工具）
 * 缓存中间件：Redis（用于缓存热点数据和分布式锁）
 * 安全框架：Spring Security + JWT（无状态认证）
 * API 文档：SpringDoc OpenAPI（Swagger 3.0）
 * 数据库：MySQL 8.0
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、业务模块
 * ═══════════════════════════════════════════════════════════════════
 *
 * 1. 英雄管理（Hero）：英雄列表、英雄详情、版本陷阱标记
 * 2. 符文推荐（Augment）：符文列表、智能推荐、套装进度
 * 3. 公告管理（Bulletin）：公告列表、最新公告、公告详情
 * 4. 社区攻略（Strategy）：攻略发布、攻略列表、投票
 * 5. 用户认证（Auth）：注册、登录、Token 刷新
 * 6. 数据管线（DataSync）：多源数据采集、聚合、验证、同步
 * 7. 管理后台（Admin）：版本陷阱标记、数据同步触发、缓存预热
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、启动流程
 * ═══════════════════════════════════════════════════════════════════
 *
 * 1. JVM 加载 AramServerApplication 类
 * 2. 调用 main 方法
 * 3. SpringApplication.run() 开始启动 Spring 容器
 * 4. 自动配置生效（读取 application.yml 配置）
 * 5. 组件扫描和 Bean 注册
 * 6. 数据源初始化（HikariCP 连接池）
 * 7. Redis 连接初始化
 * 8. DataInitializer 执行种子数据写入
 * 9. Tomcat 启动完成，开始监听 HTTP 请求
 * 10. 控制台输出启动成功日志和访问地址
 */
@SpringBootApplication
public class AramServerApplication {

    /**
     * 应用程序入口方法 —— JVM 启动时调用的第一个方法
     *
     * @param args 命令行参数（通常不使用）
     *
     * 启动命令示例：
     * java -jar aram-server.jar                    → 使用默认配置启动
     * java -jar aram-server.jar --server.port=9090 → 指定端口启动
     * java -jar aram-server.jar --spring.profiles.active=prod → 使用生产环境配置
     */
    public static void main(String[] args) {
        SpringApplication.run(AramServerApplication.class, args);
    }
}
