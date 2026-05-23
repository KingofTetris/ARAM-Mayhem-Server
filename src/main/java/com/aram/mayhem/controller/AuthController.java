package com.aram.mayhem.controller;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.dto.AuthResponse;
import com.aram.mayhem.dto.LoginRequest;
import com.aram.mayhem.dto.RefreshTokenRequest;
import com.aram.mayhem.dto.RegisterRequest;
import com.aram.mayhem.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器 —— 系统的"前台接待处"，处理用户的注册、登录和令牌刷新请求
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类是认证模块的 REST API 入口，负责接收前端的 HTTP 请求，
 * 然后把请求转发给 AuthService 处理，最后把结果包装成 Result 格式返回给前端。
 *
 * Controller 层的职责是"接待"而不是"做菜"：
 * - 接待 = 接收请求、参数校验、调用 Service、包装响应
 * - 做菜 = 实际的业务逻辑（由 AuthService 负责）
 *
 * 打个比方：
 * - Controller = 酒店前台接待员（接收客人请求，安排房间）
 * - Service = 酒店后厨（真正做菜、办理入住）
 * - Result = 统一的回复格式（标准化的入住确认单）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、API 接口一览
 * ═══════════════════════════════════════════════════════════════════
 *
 * 方法          | HTTP方法 | 路径                  | 权限     | 说明
 * --------------|---------|----------------------|---------|------------------
 * register()    | POST    | /api/auth/register   | 公开    | 用户注册
 * login()       | POST    | /api/auth/login      | 公开    | 用户登录
 * refreshToken()| POST    | /api/auth/refresh    | 公开    | 刷新令牌
 *
 * 注意：这三个接口都是"公开访问"的，不需要携带 JWT Token。
 * 因为用户还没登录，自然没有 Token 可以携带。
 * 在 SecurityConfig 中，/api/auth/** 路径被配置为 permitAll()。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、请求与响应格式
 * ═══════════════════════════════════════════════════════════════════
 *
 * 所有接口的响应都使用 Result<T> 统一包装：
 * - 成功：{ "code": 200, "message": "success", "data": {...}, "timestamp": ... }
 * - 失败：{ "code": 401/409, "message": "错误信息", "timestamp": ... }
 *
 * 请求体使用 JSON 格式，Content-Type: application/json
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、Swagger 文档注解
 * ═══════════════════════════════════════════════════════════════════
 *
 * @Tag(name = "Auth", description = "认证接口")
 *   → 在 Swagger UI 中将此 Controller 的所有接口归类到 "Auth" 分组
 *
 * @Operation(summary = "...", description = "...")
 *   → 在 Swagger UI 中显示每个接口的简要说明和详细描述
 *
 * 访问 Swagger UI：http://localhost:8080/swagger-ui.html
 *
 * 关联类：
 * @see com.aram.mayhem.service.AuthService 认证服务（实际业务逻辑）
 * @see com.aram.mayhem.common.Result 统一响应包装类
 * @see com.aram.mayhem.dto.RegisterRequest 注册请求对象
 * @see com.aram.mayhem.dto.LoginRequest 登录请求对象
 * @see com.aram.mayhem.dto.RefreshTokenRequest 刷新令牌请求对象
 * @see com.aram.mayhem.dto.AuthResponse 认证成功响应对象
 */
@Tag(name = "Auth", description = "认证接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 构造函数 —— 注入认证服务
     *
     * Spring 会自动找到 AuthService Bean 并注入进来。
     * Controller 不直接操作数据库或加密密码，所有业务逻辑都委托给 AuthService。
     *
     * @param authService 认证服务，负责注册、登录、刷新令牌的业务逻辑
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户注册接口 —— 创建新账号
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：POST（创建资源用 POST）
     * 路径：/api/auth/register
     * 权限：公开（不需要 Token）
     * Content-Type：application/json
     *
     * ═══════════════════════════════════════════════════════════════════
     * 请求体示例
     * ═══════════════════════════════════════════════════════════════════
     *
     * {
     *   "email": "player@example.com",
     *   "password": "mypassword",
     *   "nickname": "召唤师"
     * }
     *
     * ═══════════════════════════════════════════════════════════════════
     * 响应体示例（成功）
     * ═══════════════════════════════════════════════════════════════════
     *
     * {
     *   "code": 200,
     *   "message": "success",
     *   "data": {
     *     "userId": 1,
     *     "email": "player@example.com",
     *     "nickname": "召唤师",
     *     "role": "USER",
     *     "accessToken": "eyJhbGciOi...",
     *     "refreshToken": "eyJhbGciOi..."
     *   },
     *   "timestamp": 1700000000000
     * }
     *
     * ═══════════════════════════════════════════════════════════════════
     * 可能的错误
     * ═══════════════════════════════════════════════════════════════════
     *
     * - 409 Conflict：邮箱已被注册
     * - 400 Bad Request：参数校验失败（邮箱格式错误、密码太短等）
     *
     * @param request 注册请求体，@Valid 触发 Jakarta Bean Validation 自动校验
     *                - email：必填，合法邮箱格式，最长128字符
     *                - password：必填，6~64位
     *                - nickname：必填，2~64位
     * @return Result<AuthResponse> 统一响应，包含用户信息和 JWT Token 对
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "创建新账号，需要邮箱、密码和昵称")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // @Valid 注解会触发 RegisterRequest 中的校验注解（@NotBlank、@Email、@Size）
        // 如果校验失败，Spring 会自动抛出 MethodArgumentNotValidException
        // GlobalExceptionHandler 会捕获并转为 Result 格式返回给前端
        // 校验通过后，调用 AuthService 执行注册逻辑
        AuthResponse response = authService.register(request);
        // 使用 Result.success() 包装响应，统一格式返回
        return Result.success(response);
    }

    /**
     * 用户登录接口 —— 验证身份并获取 Token
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：POST
     * 路径：/api/auth/login
     * 权限：公开（不需要 Token）
     * Content-Type：application/json
     *
     * ═══════════════════════════════════════════════════════════════════
     * 请求体示例
     * ═══════════════════════════════════════════════════════════════════
     *
     * {
     *   "email": "player@example.com",
     *   "password": "mypassword"
     * }
     *
     * ═══════════════════════════════════════════════════════════════════
     * 可能的错误
     * ═══════════════════════════════════════════════════════════════════
     *
     * - 401 Unauthorized：邮箱不存在或密码错误
     *   （不区分具体原因，防止用户枚举攻击）
     *
     * @param request 登录请求体，@Valid 触发参数校验
     *                - email：必填，合法邮箱格式
     *                - password：必填
     * @return Result<AuthResponse> 统一响应，包含用户信息和 JWT Token 对
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "邮箱密码验证，返回访问令牌和刷新令牌")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // 调用 AuthService 执行登录逻辑（查询用户、验证密码、生成 Token）
        AuthResponse response = authService.login(request);
        return Result.success(response);
    }

    /**
     * 刷新令牌接口 —— 用 Refresh Token 换取新的 Token 对
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：POST
     * 路径：/api/auth/refresh
     * 权限：公开（只需要有效的 Refresh Token，不需要 Access Token）
     * Content-Type：application/json
     *
     * ═══════════════════════════════════════════════════════════════════
     * 请求体示例
     * ═══════════════════════════════════════════════════════════════════
     *
     * {
     *   "refreshToken": "eyJhbGciOi..."
     * }
     *
     * ═══════════════════════════════════════════════════════════════════
     * 使用场景
     * ═══════════════════════════════════════════════════════════════════
     *
     * 前端在 API 请求中收到 401 错误时，判断是 Access Token 过期，
     * 自动调用此接口用 Refresh Token 换取新的 Token 对，
     * 然后重试原来失败的请求。用户无感知，不需要重新登录。
     *
     * ═══════════════════════════════════════════════════════════════════
     * 可能的错误
     * ═══════════════════════════════════════════════════════════════════
     *
     * - 401 Unauthorized：Refresh Token 无效、过期或类型错误
     *
     * @param request 刷新令牌请求体，@Valid 触发参数校验
     *                - refreshToken：必填，有效的 Refresh Token 字符串
     * @return Result<AuthResponse> 统一响应，包含新的用户信息和 JWT Token 对
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用Refresh Token换取新的Access Token")
    public Result<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        // 调用 AuthService 执行令牌刷新逻辑
        // AuthService 会验证 Token 签名、有效期、类型（必须是 refresh），然后生成新 Token 对
        AuthResponse response = authService.refreshToken(request);
        return Result.success(response);
    }
}
