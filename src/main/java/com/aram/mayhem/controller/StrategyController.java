package com.aram.mayhem.controller;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.dto.CreateStrategyRequest;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.dto.StrategyDetailVO;
import com.aram.mayhem.dto.StrategyListVO;
import com.aram.mayhem.service.StrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 攻略控制器 —— 社区模块的"前台接待处"，处理攻略相关的 HTTP 请求
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类是社区模块的 REST API 入口，提供五个核心接口：
 * 1. getStrategyList()  → 分页查询攻略列表（支持热门/最新排序）
 * 2. getStrategyDetail() → 查询单个攻略详情
 * 3. createStrategy()   → 发布新攻略（需登录）
 * 4. getMyStrategies()  → 查询当前用户的攻略（需登录）
 * 5. deleteStrategy()   → 删除攻略（需登录，仅作者可删）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、API 接口一览
 * ═══════════════════════════════════════════════════════════════════
 *
 * 方法                | HTTP方法 | 路径                    | 权限     | 说明
 * --------------------|---------|------------------------|---------|------------------
 * getStrategyList()   | GET     | /api/strategies        | 公开    | 攻略列表
 * getStrategyDetail() | GET     | /api/strategies/{id}   | 公开    | 攻略详情
 * createStrategy()    | POST    | /api/strategies        | 需登录  | 发布攻略
 * getMyStrategies()   | GET     | /api/strategies/my     | 需登录  | 我的攻略
 * deleteStrategy()    | DELETE  | /api/strategies/{id}   | 需登录  | 删除攻略
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、用户身份获取方式
 * ═══════════════════════════════════════════════════════════════════
 *
 * 需要登录的接口通过 getCurrentUserId() 私有方法获取当前用户ID：
 * - 从 Spring Security 的 SecurityContextHolder 中提取 Authentication 对象
 * - Authentication.getPrincipal() 存储的是用户ID字符串（由 JwtAuthenticationFilter 设置）
 * - 如果未登录或提取失败，返回 null，接口返回 401 错误
 *
 * 关联类：
 * @see com.aram.mayhem.service.StrategyService 攻略服务（实际业务逻辑）
 * @see com.aram.mayhem.common.Result 统一响应包装类
 * @see com.aram.mayhem.dto.StrategyListVO 攻略列表项视图对象
 * @see com.aram.mayhem.dto.StrategyDetailVO 攻略详情视图对象
 * @see com.aram.mayhem.dto.CreateStrategyRequest 发布攻略请求对象
 */
@Tag(name = "Strategy", description = "攻略管理接口")
@RestController
@RequestMapping("/api/strategies")
public class StrategyController {

    private final StrategyService strategyService;

    /**
     * 构造函数 —— 注入攻略服务
     *
     * @param strategyService 攻略服务，负责攻略的增删查业务逻辑
     */
    public StrategyController(StrategyService strategyService) {
        this.strategyService = strategyService;
    }

    /**
     * 获取攻略列表接口 —— 分页查询，支持热门/最新排序
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：GET
     * 路径：/api/strategies
     * 权限：公开
     *
     * @param sort 排序方式：hot（按点赞差排序）或 latest（按发布时间排序），默认 hot
     * @param page 页码，从1开始，默认1
     * @param size 每页数量，默认10
     * @return Result<PageResult<StrategyListVO>> 分页攻略列表
     */
    @Operation(summary = "获取玩法列表", description = "支持 hot（按点赞差）和 latest（按时间）排序")
    @GetMapping
    public Result<PageResult<StrategyListVO>> getStrategyList(
            @Parameter(description = "排序方式：hot 或 latest") @RequestParam(defaultValue = "hot") String sort,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {

        // 调用 StrategyService 获取攻略列表
        List<StrategyListVO> strategies = strategyService.getStrategyList(sort, page, size);
        // 手动封装分页结果（Service 返回的是 List，需要包装为 PageResult）
        PageResult<StrategyListVO> result = new PageResult<>(strategies.size(), page, size, strategies);
        return Result.success(result);
    }

    /**
     * 获取攻略详情接口 —— 根据ID查询完整攻略内容
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：GET
     * 路径：/api/strategies/{id}
     * 权限：公开
     *
     * @param id 攻略ID，通过 @PathVariable 从 URL 路径中提取
     * @return Result<StrategyDetailVO> 攻略详情，不存在返回 404
     */
    @Operation(summary = "获取玩法详情")
    @GetMapping("/{id}")
    public Result<StrategyDetailVO> getStrategyDetail(
            @Parameter(description = "玩法ID") @PathVariable Long id) {

        // 调用 StrategyService 获取攻略详情
        StrategyDetailVO strategy = strategyService.getStrategyDetail(id);
        // 如果查询结果为 null，说明攻略不存在
        if (strategy == null) {
            return Result.error(404, "玩法不存在");
        }
        return Result.success(strategy);
    }

    /**
     * 发布攻略接口 —— 创建新的游戏攻略
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：POST（创建资源用 POST）
     * 路径：/api/strategies
     * 权限：需登录（需要 JWT Token）
     * Content-Type：application/json
     *
     * ═══════════════════════════════════════════════════════════════════
     * 事务说明
     * ═══════════════════════════════════════════════════════════════════
     *
     * 创建攻略需要同时写入三张表：
     * 1. tb_strategy（攻略主体）
     * 2. tb_strategy_augment（攻略-符文关联）
     * 3. tb_strategy_item（攻略-装备关联）
     * StrategyService 使用 @Transactional 保证原子性，任一失败全部回滚。
     *
     * @param request 发布攻略请求体，包含：
     *                - heroId：关联的英雄ID
     *                - title：攻略标题
     *                - description：攻略描述
     *                - augmentIds：关联的符文ID列表
     *                - itemIds：关联的装备ID列表
     * @return Result<StrategyDetailVO> 发布后的攻略详情
     */
    @Operation(summary = "发布玩法", description = "需要登录")
    @PostMapping
    public Result<StrategyDetailVO> createStrategy(@Valid @RequestBody CreateStrategyRequest request) {
        // 从 SecurityContext 中获取当前登录用户ID
        Long userId = getCurrentUserId();
        if (userId == null) {
            // 未登录，返回 401 错误
            return Result.error(401, "请先登录");
        }

        try {
            // 调用 StrategyService 创建攻略（含事务管理）
            // 事务内会同时写入攻略主体、符文关联和装备关联
            StrategyDetailVO strategy = strategyService.createStrategy(
                    userId,
                    request.getHeroId(),
                    request.getTitle(),
                    request.getDescription(),
                    request.getAugmentIds(),
                    request.getItemIds()
            );
            return Result.success(strategy);
        } catch (IllegalArgumentException e) {
            // 参数校验失败或业务规则不满足（如英雄不存在、符文ID无效等）
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 获取我的投稿接口 —— 查询当前用户发布的所有攻略
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：GET
     * 路径：/api/strategies/my
     * 权限：需登录
     *
     * @return Result<List<StrategyListVO>> 当前用户发布的攻略列表
     */
    @Operation(summary = "获取我的投稿")
    @GetMapping("/my")
    public Result<List<StrategyListVO>> getMyStrategies() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        // 调用 StrategyService 获取指定用户的攻略列表
        List<StrategyListVO> strategies = strategyService.getUserStrategies(userId);
        return Result.success(strategies);
    }

    /**
     * 删除攻略接口 —— 删除指定攻略及其关联数据
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：DELETE
     * 路径：/api/strategies/{id}
     * 权限：需登录，仅攻略作者可删除
     *
     * ═══════════════════════════════════════════════════════════════════
     * 权限校验说明
     * ═══════════════════════════════════════════════════════════════════
     *
     * StrategyService.deleteStrategy() 内部会校验：
     * 1. 攻略是否存在
     * 2. 当前用户是否是攻略的作者
     * 只有作者才能删除自己的攻略，防止他人恶意删除。
     *
     * @param id 攻略ID
     * @return Result<Void> 删除结果
     */
    @Operation(summary = "删除攻略", description = "需要登录，仅作者可删除")
    @DeleteMapping("/{id}")
    public Result<Void> deleteStrategy(@Parameter(description = "攻略ID") @PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        try {
            // 调用 StrategyService 删除攻略
            // Service 层会校验当前用户是否是攻略作者
            strategyService.deleteStrategy(id, userId);
            return Result.success(null);
        } catch (IllegalArgumentException e) {
            // 权限不足（非作者）或攻略不存在
            return Result.error(403, e.getMessage());
        }
    }

    /**
     * 获取当前登录用户ID —— 从 Spring Security 上下文中提取
     *
     * ═══════════════════════════════════════════════════════════════════
     * 工作原理
     * ═══════════════════════════════════════════════════════════════════
     *
     * 1. JwtAuthenticationFilter 在请求进入时，从 Authorization 头中提取 JWT Token
     * 2. 解析 Token 获取用户ID，创建 Authentication 对象
     * 3. 将 Authentication 存入 SecurityContextHolder
     * 4. 本方法从 SecurityContextHolder 中取出 Authentication，提取用户ID
     *
     * Authentication.getPrincipal() 返回的是用户ID字符串，
     * 这是 JwtAuthenticationFilter 在创建 Authentication 时设置的。
     *
     * @return 用户ID，如果未登录或提取失败返回 null
     */
    private Long getCurrentUserId() {
        // SecurityContextHolder 是 Spring Security 的核心容器
        // 它保存了当前请求的安全上下文（包括认证信息）
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            try {
                // getPrincipal() 返回的是用户ID字符串，需要转换为 Long
                return Long.parseLong(authentication.getPrincipal().toString());
            } catch (NumberFormatException e) {
                // 如果 principal 不是有效的数字字符串，返回 null
                return null;
            }
        }
        return null;
    }
}