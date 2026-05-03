# ARAM Mayhem Server - 后端服务

> 海克斯大乱斗信息差助手 - Spring Boot 后端服务

[![GitHub](https://img.shields.io/badge/github-KingofTetris/ARAM--Mayhem--Server-blue?logo=github)
[![License](https://img.shields.io/badge/license-MIT-green)
[![Java Version](https://img.shields.io/badge/java-21-orange)
[![Spring Boot](https://img.shields.io/badge/spring%20boot-3.3.5-brightgreen)

## 项目概述

**ARAM Mayhem Server** 是《英雄联盟》海克斯大乱斗信息差助手的后端服务，提供 RESTful API，支持用户认证、英雄数据、玩法策略社区等功能。

### 主要功能

- 🔐 **JWT 认证系统**：Access Token + Refresh Token 双令牌机制
- 👤 **用户管理**：注册、登录、个人信息管理
- 📊 **英雄数据API**：英雄信息、强度分级、英雄修饰效果
- 💎 **强化符文API**：Hextech ARAM 强化符文数据
- 🎮 **玩法策略**：发布策略、评论、投票
- 📢 **公告系统**：系统公告管理
- 🚀 **高性能缓存**：Redis 缓存热点数据
- 🌐 **CORS 支持**：跨域访问配置

## 技术栈

| 技术 | 版本/选型 | 用途 |
|------|----------|------|
| **编程语言** | Java 21 | 核心开发语言 |
| **应用框架** | Spring Boot 3.3.5 | 应用框架 |
| **ORM框架** | MyBatis Plus 3.5.9 | 数据库持久化 |
| **安全框架** | Spring Security + JWT (jjwt 0.12.6) | 认证与授权 |
| **数据库** | MySQL 8.0+ (utf8mb4) | 主数据库 |
| **缓存** | Redis 3.0+ | 热点数据缓存 |
| **文档** | SpringDoc OpenAPI 2.6.0 | API文档生成 |
| **构建工具** | Maven 3.9.15 | 项目构建 |

## 项目结构

```
aram-server/
├── src/main/java/com/aram/mayhem/
│   ├── AramServerApplication.java          # 应用启动类
│   ├── common/                             # 公共工具
│   │   ├── BusinessException.java         # 业务异常
│   │   ├── GlobalExceptionHandler.java    # 全局异常处理
│   │   └── Result.java                   # 统一响应封装
│   ├── config/                             # 配置类
│   │   ├── CorsConfig.java                # 跨域配置
│   │   ├── RedisConfig.java               # Redis配置
│   │   └── SecurityConfig.java            # Spring Security配置
│   ├── controller/                        # 控制器层
│   │   └── AuthController.java            # 认证接口
│   ├── dto/                               # 数据传输对象
│   │   ├── AuthResponse.java             # 认证响应
│   │   ├── LoginRequest.java             # 登录请求
│   │   ├── RegisterRequest.java          # 注册请求
│   │   └── RefreshTokenRequest.java       # Token刷新请求
│   ├── entity/                            # 实体类
│   │   ├── User.java
│   │   ├── Hero.java
│   │   ├── HeroModifier.java
│   │   ├── Augment.java
│   │   ├── Strategy.java
│   │   ├── StrategyItem.java
│   │   ├── StrategyAugment.java
│   │   ├── Vote.java
│   │   └── Bulletin.java
│   ├── mapper/                            # MyBatis Mapper
│   │   ├── UserMapper.java
│   │   ├── HeroMapper.java
│   │   └── ... (其他Mapper)
│   ├── security/                          # 安全相关
│   │   ├── CustomUserDetailsService.java  # 用户详情服务
│   │   ├── JwtAuthenticationFilter.java   # JWT过滤器
│   │   └── JwtTokenProvider.java          # JWT工具
│   └── service/                           # 业务层
│       └── AuthService.java              # 认证服务
├── src/main/resources/
│   ├── application.yml                   # 主配置
│   └── application-local.yml             # 本地开发配置
├── target/                                # 构建输出 (git忽略)
├── pom.xml
├── start-server.bat                        # Windows启动脚本
└── README.md
```

## 数据库设计

### 核心实体

| 实体 | 说明 |
|------|------|
| `user` | 用户表（用户名、密码哈希、邮箱等） |
| `hero` | 英雄表（英雄名称、标题、描述、强度分级） |
| `hero_modifier` | 英雄修饰效果（Hextech ARAM特定修改） |
| `augment` | 强化符文表 |
| `strategy` | 玩法策略表（标题、内容、作者、点赞数） |
| `strategy_item` | 策略出装项 |
| `strategy_augment` | 策略搭配强化 |
| `vote` | 投票表（对策略点赞/踩） |
| `bulletin` | 公告表 |

### 字符集

```sql
CREATE DATABASE aram_mayhem DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 环境要求

### 开发环境

- **JDK**: 21 (Oracle OpenJDK 21.0.10)
- **Maven**: 3.9.15
- **MySQL**: 8.0+
- **Redis**: 3.0+ (Windows可用 Memurai 或 Redis for Windows)
- **IDE**: IntelliJ IDEA 2023.3+ (推荐)

### 生产环境

- **JDK**: 21
- **MySQL**: 8.0+
- **Redis**: 5.0+
- **部署方式**: JAR包 或 Docker容器

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/KingofTetris/ARAM-Mayhem-Server.git
cd ARAM-Mayhem-Server
```

### 2. 配置数据库和Redis

修改 `src/main/resources/application-local.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/aram_mayhem?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 你的密码
  data:
    redis:
      host: localhost
      port: 6379
```

### 3. 初始化数据库

```sql
-- 1. 创建数据库
CREATE DATABASE aram_mayhem DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. MyBatis Plus 会自动生成表结构（运行项目后）
```

### 4. 启动服务

#### Windows (推荐)

```bash
# 直接使用脚本
start-server.bat
```

#### 命令行启动

```bash
# 编译打包
mvn clean package -DskipTests

# 运行JAR
java -jar target/aram-server-1.0.0.jar

# 或直接使用Maven运行
mvn spring-boot:run
```

#### IDEA 启动

1. 打开 IntelliJ IDEA
2. File → Open → 选择项目目录
3. 等待 Maven 依赖下载
4. 找到 `AramServerApplication.java`
5. 右键 → Run 'AramServerApplication'

### 5. 验证服务

服务启动后访问：

- 🌐 **服务地址**: http://localhost:8080
- 📚 **API文档**: http://localhost:8080/swagger-ui.html
- 🏥 **健康检查**: http://localhost:8080/actuator/health

## API 文档

完整API文档通过 **Swagger UI** 访问：`http://localhost:8080/swagger-ui.html`

### 核心接口

#### 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/login` | 用户登录 |
| POST | `/api/auth/refresh` | 刷新Token |

#### 响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 900
  }
}
```

### JWT Token 使用

```bash
# 请求需要认证的API时，在Header中添加
Authorization: Bearer <你的access_token>
```

## 配置说明

### 配置文件

- `application.yml`: 主配置文件
- `application-local.yml`: 本地开发环境（已配置为不使用profile）
- `application-prod.yml`: 生产环境（可选）

### 主要配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | 8080 | 服务端口 |
| `jwt.secret` | 随机生成 | JWT签名密钥 |
| `jwt.access-token-expiration` | 900s (15m) | Access Token有效期 |
| `jwt.refresh-token-expiration` | 604800s (7d) | Refresh Token有效期 |
| `spring.datasource.url` | localhost:3306 | MySQL地址 |
| `spring.data.redis.host` | localhost | Redis地址 |

## 开发指南

### 添加新API

1. **创建 DTO** → `dto/` 包
2. **创建 Service** → `service/` 包
3. **创建 Controller** → `controller/` 包
4. **添加接口文档注解**（Swagger/OpenAPI）

### 代码规范

- 遵循 **阿里巴巴Java开发规范**
- 使用 `Result` 统一响应
- 业务异常使用 `BusinessException`
- 使用 MyBatis Plus `@TableField` 和 `@TableId`

### 提交规范

```bash
# 提交格式
git commit -m "feat: 添加英雄列表API"

# 类型
feat: 新功能
fix: 修复bug
docs: 文档
style: 格式
refactor: 重构
test: 测试
chore: 构建/工具
```

## 部署指南

### 生产部署

```bash
# 1. 打包
mvn clean package -DskipTests -Pprod

# 2. 上传JAR到服务器
# target/aram-server-1.0.0.jar

# 3. 运行（使用systemd）
cat > /etc/systemd/system/aram-server.service << 'EOF'
[Unit]
Description=ARAM Mayhem Server

[Service]
Type=simple
WorkingDirectory=/opt/aram-server
ExecStart=/usr/bin/java -jar /opt/aram-server/aram-server-1.0.0.jar
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

# 4. 启动服务
systemctl enable aram-server
systemctl start aram-server
```

### Docker 部署 (可选)

```dockerfile
# Dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/aram-server-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 贡献指南

我们欢迎任何形式的贡献！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: 添加某个功能'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

## 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

```
MIT License

Copyright (c) 2026 KingofTetris

Permission is hereby granted, free of charge...
```

## 致谢

- 拳头游戏 (Riot Games)
- Spring Boot / MyBatis Plus 社区
- 所有开源库作者

---

## 联系方式

- 项目问题：[GitHub Issues](https://github.com/KingofTetris/ARAM-Mayhem-Server/issues)
- 邮箱：1204066670@qq.com
- 关联项目：
  - [项目工作区](https://github.com/KingofTetris/ARAM-Mayhem-Assistant)
  - [Android客户端](https://github.com/KingofTetris/ARAM-Mayhem-Android)
