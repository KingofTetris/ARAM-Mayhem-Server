# ARAM Mayhem Assistant 开发操作步骤记录

> 本文档记录项目从初始化到当前阶段的所有关键操作步骤、决策过程、问题及解决方案。
> 最后更新：2026-05-03

---

## 阶段一：项目初始化（M1）

### 1.1 项目脚手架搭建

**开始时间**：2026-05-03 09:00:00

**实施内容**：

1. 创建 Android 项目目录结构 `D:\androidProjects\ARAM_Mayhem_Assistant`
2. 创建 11 个 Gradle 模块：app, core-common, core-network, core-data, core-ui, feature-hero, feature-augment, feature-community, feature-profile, feature-bulletin, navigation
3. 创建 Spring Boot 后端项目 `D:\ideaProjects\aram-server`
4. 编写 `settings.gradle`（11 模块 include）、根 `build.gradle`（统一依赖版本 ext{}）
5. 编写各模块独立 `build.gradle`（app=application, core-common=java-library, 其余=android-library）
6. 编写 `gradle-wrapper.properties`（Gradle 8.7）、`gradle.properties`、`gradlew.bat`

**使用工具**：Write 工具逐文件创建、RunCommand 创建目录结构

**关键决策**：
- **决策1**：采用多模块架构而非单模块，理由：feature 模块可独立编译、降低增量构建时间
- **决策2**：core-common 使用 `java-library` 插件而非 `android-library`，理由：纯 Java 类（Constants/Result/Tier）无需 Android SDK
- **决策3**：Gradle 使用 Groovy DSL 而非 Kotlin DSL，理由：与 tasks.md 规格一致，Groovy 生态更成熟

**遇到的问题及解决**：
| 问题 | 解决方案 |
|------|----------|
| Gradle wrapper JAR 文件缺失 | 从本地缓存 `C:\Users\12040\.gradle\wrapper\dists\gradle-8.7-bin` 中找到已安装的 Gradle 8.7，执行 `gradle wrapper` 命令生成 |
| sandbox 无法删除 navigation 模块目录 | 从 settings.gradle 和 app/build.gradle 中移除引用即可，Gradle 不会编译未 include 的模块 |

---

### 1.2 开发环境与工具链配置

**开始时间**：2026-05-03 12:00:00

**实施内容**：

1. 配置 Android Gradle 多模块依赖关系（app → core×4 + feature×5 + navigation）
2. 编写后端 `pom.xml`：Spring Boot 3.3.5 + MyBatis Plus 3.5.9 + Spring Security + jjwt 0.12.6 + SpringDoc 2.6.0
3. 编写 `application.yml`：MySQL(127.0.0.1:3306/aram_mayhem) + Redis(127.0.0.1:6379) + JWT 密钥 + MyBatis Plus 配置
4. 编写 `application-local.yml`：DEBUG 日志级别
5. Android 配置 ViewBinding（app + 5 个 feature 模块 `buildFeatures { viewBinding true }`）
6. Android 配置 Hilt（app + core-network + core-data + 5 个 feature 模块添加 Hilt 插件和依赖）
7. 创建 `network_security_config.xml`（允许 192.168.1.100 和 localhost 明文 HTTP）
8. 在 `AndroidManifest.xml` 中引用 `android:networkSecurityConfig`
9. 创建 `Constants.java` 定义 `BASE_URL = "http://192.168.1.100:8080/"`

**使用工具**：Write 工具、RunCommand 验证 Maven

**关键决策**：
- **决策4**：JWT 密钥使用 Base64 编码存储在 yml 中，而非环境变量，理由：本地部署场景无需云环境变量
- **决策5**：MySQL 绑定 127.0.0.1 而非 0.0.0.0，理由：本地开发安全，外部通过防火墙规则控制
- **决策6**：Access Token 15 分钟 + Refresh Token 7 天，理由：移动端使用频率高，短 Access Token 保障安全，长 Refresh Token 减少重新登录

**遇到的问题及解决**：
| 问题 | 解决方案 |
|------|----------|
| Maven 不在 sandbox PATH 中 | 使用完整路径 `D:\dev\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd` 执行 |
| sandbox PowerShell 无法读取用户/系统环境变量 | 承认限制，不依赖环境变量，直接使用已知安装路径 |

---

## 阶段二：基础架构（M2）

### 2.1 MySQL 数据库建模（Task 3）

**开始时间**：2026-05-03 18:00:00

**实施内容**：

1. 通过 MCP MySQL 工具执行 DDL，创建 `aram_mayhem` 数据库（utf8mb4_unicode_ci）
2. 创建 9 张表：tb_user, tb_hero, tb_hero_modifier, tb_augment, tb_strategy, tb_strategy_augment, tb_strategy_item, tb_vote, tb_bulletin
3. 创建 10 个索引：idx_hero_tier, idx_hero_role, idx_hero_name_zh, idx_augment_quality, idx_augment_synergy, idx_strategy_hero, idx_strategy_user, idx_strategy_hot(表达式索引), idx_bulletin_type, idx_bulletin_pinned
4. 创建 9 个 Entity 类（@TableName + @TableId + @TableField + 逻辑删除 deleted）
5. 创建 9 个 Mapper 接口（extends BaseMapper<T>）

**使用工具**：MCP MySQL execute_sql、backend-android-collaboration-expert 智能体

**关键决策**：
- **决策7**：所有表使用 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`，理由：utf8mb4 支持完整 Unicode（含 emoji），utf8mb4_unicode_ci 排序规则准确
- **决策8**：tb_strategy_hot 使用表达式索引 `(upvotes - downvotes)`，理由：MySQL 8.0+ 支持函数索引，避免查询时计算
- **决策9**：逻辑删除字段 `deleted TINYINT DEFAULT 0`，理由：MyBatis Plus @TableLogic 自动处理，数据可恢复

**遇到的问题及解决**：
| 问题 | 解决方案 |
|------|----------|
| TEXT 列不能设置 DEFAULT 值（MySQL 严格模式） | 移除 tb_augment.description 和 tb_bulletin.content 的 DEFAULT '' |

---

### 2.2 后端 RESTful API 框架（Task 4）

**开始时间**：2026-05-03 18:00:00

**实施内容**：

1. 创建 `Result<T>` 统一响应体（code + message + data + timestamp）
2. 创建 `GlobalExceptionHandler`（@RestControllerAdvice 处理 Validation/Business/Unknown 异常）
3. 创建 `BusinessException` 自定义异常
4. 创建 `CorsConfig`（CorsFilter 允许所有 Origin + 标准方法 + 凭证）
5. 创建 `RedisConfig`（RedisTemplate + Jackson2JsonRedisSerializer + Java8 时间模块）
6. SpringDoc OpenAPI 已通过 pom.xml 依赖自动配置

**使用工具**：backend-android-collaboration-expert 智能体

**关键决策**：
- **决策10**：CORS 允许 `*` Origin，理由：本地局域网部署，无跨域安全风险
- **决策11**：Redis Value 使用 Jackson JSON 序列化而非 JDK 序列化，理由：可读性好、跨语言兼容、体积更小

---

### 2.3 Spring Security + JWT 认证（Task 5）

**开始时间**：2026-05-03 18:00:00

**实施内容**：

1. 创建 `JwtTokenProvider`（HMAC-SHA 密钥签名，Access 15min + Refresh 7d）
2. 创建 `JwtAuthenticationFilter`（OncePerRequestFilter，Bearer Token 提取 + 验证 + SecurityContext 注入）
3. 创建 `SecurityConfig`（CSRF 禁用 + STATELESS 会话 + /api/auth/** 放行 + 其余需认证）
4. 创建 `CustomUserDetailsService`（UserDetailsService 实现，email 查询 + BCrypt 密码验证）
5. 创建 4 个 DTO：RegisterRequest, LoginRequest, RefreshTokenRequest, AuthResponse
6. 创建 `AuthService`（register/login/refresh 完整业务逻辑）
7. 创建 `AuthController`（POST /api/auth/register, /api/auth/login, /api/auth/refresh）
8. 后端添加 Lombok 依赖到 pom.xml

**使用工具**：backend-android-collaboration-expert 智能体、MCP MySQL

**关键决策**：
- **决策12**：SecurityFilterChain 使用 Lambda DSL 而非 WebSecurityConfigurerAdapter，理由：Spring Security 6.x 已废弃后者
- **决策13**：JwtAuthenticationFilter 注入 UserDetailsService 加载完整权限，而非仅存储 userId，理由：后续 RBAC 权限控制需要角色信息

**遇到的问题及解决**：
| 问题 | 解决方案 |
|------|----------|
| JwtAuthenticationFilter 初始版本仅设置 userId 为 principal | 更新为注入 UserDetailsService，加载完整 UserDetails 含权限列表 |

**编译验证**：`mvn clean compile -DskipTests` → **34 源文件编译通过，BUILD SUCCESS**

---

## 阶段三：Android UI 框架（M3）

### 3.1 Android 核心模块搭建（Task 6）

**开始时间**：2026-05-03 09:00:00

**实施内容**：

1. 创建 `MayhemApplication.java`（@HiltAndroidApp）
2. 创建 `MainActivity.java`（@AndroidEntryPoint + NavHostFragment + BottomNavigationView）
3. 创建 `activity_main.xml`（ConstraintLayout + FragmentContainerView + BottomNavigationView）
4. 创建 `navigation_graph.xml`（5 Tab：英雄/符文/玩法/公告/我的）
5. 创建 `bottom_nav_menu.xml`（5 个 item + 图标引用）
6. 创建 5 个 Fragment 占位：HeroListFragment, AugmentListFragment, CommunityFeedFragment, BulletinListFragment, ProfileFragment
7. 创建 5 个 Fragment 布局文件（ConstraintLayout + TextView 占位）
8. 创建 `core-common` 3 个 Java 类：Constants, Result, Tier
9. 创建 `core-ui` 完整资源体系：colors.xml, strings.xml, dimens.xml, themes.xml
10. 创建 13 个 drawable 资源：5 个底部导航图标 + 2 个启动图标 + 6 个功能性 drawable
11. 创建 `network_security_config.xml` + `proguard-rules.pro`

**使用工具**：Write 工具批量创建、RunCommand 创建目录

**关键决策**：
- **决策14**：Tier 枚举使用 @SerializedName 注解，理由：Gson 反序列化需要映射 S_PLUS → "S_PLUS" 字符串
- **决策15**：5 个 Fragment 布局使用 ConstraintLayout 而非 LinearLayout，理由：ConstraintLayout 性能更优，复杂布局层级更少
- **决策16**：底部导航图标使用 Vector Drawable 而非 PNG，理由：矢量图任意缩放不失真，APK 体积更小

---

### 3.2 资源缺失问题修复

**开始时间**：2026-05-03 18:00:00

**问题诊断**：

| 问题 | 根因 | 严重程度 |
|------|------|----------|
| feature 模块引用 @color/background 等资源无法解析 | 共享资源仅存在于 app 模块 | 🔴 致命 |
| 所有 library 模块缺少 AndroidManifest.xml | AGP 要求每个模块必须有 manifest | 🔴 致命 |
| navigation_graph.xml 在 navigation 模块无法解析 Fragment 类 | navigation 模块不依赖 feature 模块 | 🟠 严重 |
| 启动图标 adaptive-icon 引用 @color/ 而非 @drawable/ | 颜色资源不能直接作为 adaptive-icon 的 drawable | 🟡 中等 |
| core-common 缺少 Gson 依赖 | Result.java 使用 @SerializedName 但 build.gradle 无 Gson | 🔴 致命 |
| gradle-wrapper.jar 缺失 | 手动创建项目时未生成 wrapper JAR | 🔴 致命 |

**修复步骤**：

1. **共享资源迁移**：将 app 模块的 colors.xml(25色值)、strings.xml(35字符串)、dimens.xml(30尺寸)、themes.xml(14样式) 迁移到 core-ui 模块
2. **图标迁移**：5 个底部导航图标 drawable 从 app 迁移到 core-ui
3. **新增 drawable**：ic_launcher_foreground.xml、ic_launcher_background.xml、bg_trap_warning.xml、bg_card.xml、divider_horizontal.xml、bg_tier_s_plus/s/a/b/c.xml
4. **AndroidManifest 补全**：为 9 个 library 模块各创建 AndroidManifest.xml
5. **导航图迁移**：navigation_graph.xml 从 navigation 模块迁移到 app 模块（app 依赖所有 feature 模块，可解析 Fragment 类引用）
6. **启动图标修复**：adaptive-icon 引用改为 `@drawable/ic_launcher_foreground` 和 `@drawable/ic_launcher_background`
7. **app 模块旧资源清理**：移除已迁移的 colors/dimens/themes（保留空 resources 标签），移除 5 个图标 drawable
8. **settings.gradle 更新**：移除 `include ':navigation'`
9. **app/build.gradle 更新**：移除 `implementation project(':navigation')`
10. **core-common/build.gradle 更新**：添加 `implementation "com.google.code.gson:gson:$gsonVersion"`
11. **Gradle wrapper 生成**：从本地缓存执行 `gradle wrapper --gradle-version 8.7`

**使用工具**：Write、DeleteFile、RunCommand、Glob、Grep

**关键决策**：
- **决策17**：共享资源放在 core-ui 而非 app 模块，理由：feature 模块依赖 core-ui 而非 app，资源通过依赖链自动合并
- **决策18**：导航图放在 app 模块而非独立 navigation 模块，理由：app 依赖所有 feature 模块，可以解析 Fragment 类引用；独立模块无法做到
- **决策19**：app 模块的 colors/dimens/themes 保留空 `<resources />` 标签而非删除文件，理由：避免未来 app 特有资源无处放置

**编译验证**：`gradlew assembleDebug` → **BUILD SUCCESSFUL, 284 tasks, app-debug.apk 7.8 MB**

---

## 进度跟踪体系建立

**开始时间**：2026-05-03

**实施内容**：

1. 创建 `project_progress_tracker.xlsx`（5 个 Sheet）
2. 使用 openpyxl 生成，包含：总体概览、安卓端进度、后端进度、更新日志、里程碑概览
3. 同步分发到三个位置：Android 项目目录、后端项目目录、规划目录
4. 创建 `create_tracker.py` 脚本支持一键重新生成

**关键决策**：
- **决策20**：安卓端和后端使用独立 Sheet 而非合并，理由：两端进展差异大（安卓 55% vs 后端 100%），独立展示更真实
- **决策21**：每个 Sheet 分 A/B/C 三段（已完成/待完成/阶段计划），理由：结构清晰，便于快速定位当前状态
- **决策22**：使用 Python 脚本生成 xlsx 而非手动维护，理由：可版本控制、可自动化更新

---

## 关键技术参数汇总

| 参数 | 值 |
|------|-----|
| JDK 版本 | 21 (21.0.10) |
| Android Gradle Plugin | 8.5.2 |
| Gradle 版本 | 8.7 |
| compileSdk / targetSdk | 34 |
| minSdk | 26 |
| Spring Boot | 3.3.5 |
| MyBatis Plus | 3.5.9 |
| jjwt | 0.12.6 |
| SpringDoc | 2.6.0 |
| Hilt | 2.51.1 |
| Retrofit | 2.11.0 |
| OkHttp | 4.12.0 |
| Room | 2.6.1 |
| Glide | 4.16.0 |
| Navigation | 2.7.7 |
| MySQL | 9.7.0 |
| Redis | 3.0.504 (MSOpenTech Windows) |
| Maven | 3.9.15 |
| 数据库字符集 | utf8mb4 / utf8mb4_unicode_ci |
| 后端端口 | 8080 |
| MySQL 端口 | 3306 |
| Redis 端口 | 6379 |
| Access Token 有效期 | 15 分钟 |
| Refresh Token 有效期 | 7 天 |

---

## 待办事项（按优先级）

1. **[高] M3 安卓端**：8 个通用 UI 组件开发（TierBadgeView, HeroCardAdapter, AugmentCardAdapter 等）
2. **[高] M3 安卓端**：AuthInterceptor + TokenRefreshInterceptor 实现
3. **[高] M3 安卓端**：Retrofit API 接口定义 + Room Database 实现
4. **[高] M4 后端**：DataInitializer 种子数据 + HeroController + HeroService
5. **[中] M4 后端**：HeroService Redis 缓存策略
6. **[低] M1 遗留**：Checkstyle + SpotBugs 代码规范配置
