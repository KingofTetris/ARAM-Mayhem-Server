# ARAM Mayhem Server - 后端服务

> 海克斯大乱斗信息差助手 - Spring Boot 后端 API 服务

[![GitHub](https://img.shields.io/badge/github-KingofTetris/ARAM--Mayhem--Server-blue?logo=github)
[![License](https://img.shields.io/badge/license-MIT-green)
[![Java Version](https://img.shields.io/badge/java-21-orange)
[![Spring Boot](https://img.shields.io/badge/spring%20boot-3.3.5-brightgreen)

## 快速开始

### 环境要求

- JDK 21
- Maven 3.9+
- MySQL 8.0+
- Redis 3.0+（可选）

### 配置

修改 `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/aram_mayhem
    username: root
    password: your_password
```

### 构建

```bash
mvn clean package
```

### 运行

```bash
# 开发模式
mvn spring-boot:run

# 或打包后运行
java -jar target/aram-server-1.0.0.jar
```

服务地址: http://localhost:8080

## 技术栈

| 技术 | 版本 |
|------|------|
| Java | 21 |
| Spring Boot | 3.3.5 |
| MyBatis Plus | 3.5.9 |
| Spring Security + JWT | - |
| MySQL | 8.0+ |
| Redis | 3.0+ |

## API 文档

启动后访问以下任一公开接口：

```bash
# 英雄列表（公开接口）
http://localhost:8080/api/heroes

# 或访问 Swagger UI（推荐）
http://localhost:8080/swagger-ui/index.html
```

如果能看到 JSON 数据或 Swagger UI 页面，说明启动成功。

- OpenAPI: http://localhost:8080/v3/api-docs

## 项目结构

```
src/main/java/com/aram/mayhem/
├── AramServerApplication.java  # 启动类
├── common/                    # 公共类
├── config/                    # 配置类
├── controller/               # 控制器
├── dto/                      # 数据传输对象
├── entity/                   # 实体类
├── mapper/                   # 数据库映射
├── security/                 # 安全认证
└── service/                  # 业务逻辑
```

## 配套项目

- [Android 客户端](https://github.com/KingofTetris/ARAM-Mayhem-Android) - 手机 App
- [详细入门指南](./小白入门指南.md) - 新手完整安装配置教程

## 许可证

MIT

## 作者

[KingofTetris](https://github.com/KingofTetris)
