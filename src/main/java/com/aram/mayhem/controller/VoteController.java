package com.aram.mayhem.controller;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.dto.VoteRequest;
import com.aram.mayhem.service.StrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 投票控制器 —— 管理社区攻略的点赞/点踩功能
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类是社区模块中"投票"功能的 REST API 入口，提供：
 * 1. vote()       → 对攻略进行点赞(UP)或点踩(DOWN)
 * 2. cancelVote() → 取消已投的票
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、权限说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 所有接口都需要登录，未登录用户无法投票。
 * 用户身份通过 JWT Token 传递，在 JwtAuthenticationFilter 中解析后
 * 存入 SecurityContext，本控制器通过 getCurrentUserId() 提取。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、投票业务规则
 * ═══════════════════════════════════════════════════════════════════
 *
 * 1. 同一用户对同一攻略只能有一票
 * 2. 重复投票会切换投票状态（UP→DOWN 或 DOWN→UP）
 * 3. 取消投票后，可以重新投票
 * 4. 投票类型只有两种：UP（点赞）和 DOWN（点踩）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、URL 设计说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 路径前缀：/api/strategies/{strategyId}/vote
 *
 * 采用嵌套资源路径设计，因为"投票"是"攻略"的子资源：
 * - 投票不能独立存在，必须依附于某个攻略
 * - {strategyId} 是路径参数，指定对哪个攻略投票
 *
 * RESTful 语义：
 * - POST   /api/strategies/{id}/vote  → 创建投票（点赞/点踩）
 * - DELETE /api/strategies/{id}/vote  → 删除投票（取消投票）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * 前端社区页面 → HTTP 请求 → VoteController → StrategyService → StrategyMapper → MySQL
 *
 * 关联类：
 * - StrategyService：攻略业务逻辑层，包含投票逻辑
 * - VoteRequest：投票请求体，包含 voteType（UP/DOWN）
 */
@Tag(name = "投票", description = "玩法投票接口")
@RestController
@RequestMapping("/api/strategies/{strategyId}/vote")
public class VoteController {

    private final StrategyService strategyService;

    /**
     * 依赖注入：攻略业务逻辑服务
     *
     * 投票逻辑实际在 StrategyService 中实现，因为投票是攻略的附属功能。
     * VoteController 只负责接收请求和提取用户身份，具体业务逻辑委托给 StrategyService。
     */
    public VoteController(StrategyService strategyService) {
        this.strategyService = strategyService;
    }

    /**
     * 对攻略进行投票 —— 点赞(UP)或点踩(DOWN)
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：POST（创建投票用 POST，符合 RESTful 规范）
     * 路径：/api/strategies/{strategyId}/vote
     * 权限：需登录（从 JWT Token 提取用户ID）
     *
     * ═══════════════════════════════════════════════════════════════════
     * 请求体说明
     * ═══════════════════════════════════════════════════════════════════
     *
     * VoteRequest 包含一个字段 voteType：
     * - "UP"：点赞，表示用户认为这个攻略有用
     * - "DOWN"：点踩，表示用户认为这个攻略没用
     *
     * ═══════════════════════════════════════════════════════════════════
     * 重复投票处理
     * ═══════════════════════════════════════════════════════════════════
     *
     * 如果用户已经投过票：
     * - 投相同类型（已UP再UP）：忽略，不重复计算
     * - 投不同类型（已UP再DOWN）：切换投票状态
     *
     * @param strategyId 攻略ID（路径参数，指定对哪个攻略投票）
     * @param request    投票请求体，包含 voteType（UP/DOWN）
     * @return Result<Void> 投票成功返回空数据，失败返回错误信息
     */
    @Operation(summary = "投票", description = "对玩法进行投票，UP 为点赞，DOWN 为点踩")
    @PostMapping
    public Result<Void> vote(
            @Parameter(description = "玩法ID") @PathVariable Long strategyId,
            @Valid @RequestBody VoteRequest request) {

        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        try {
            strategyService.vote(strategyId, userId, request.getVoteType());
            return Result.success();
        } catch (IllegalStateException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 取消投票 —— 撤销已投的点赞或点踩
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：DELETE（删除投票用 DELETE，符合 RESTful 规范）
     * 路径：/api/strategies/{strategyId}/vote
     * 权限：需登录
     *
     * ═══════════════════════════════════════════════════════════════════
     * 取消后的效果
     * ═══════════════════════════════════════════════════════════════════
     *
     * 取消投票后：
     * - 该攻略的点赞数或点踩数会减1
     * - 用户可以重新对该攻略投票
     * - 如果用户之前没有投过票，调用此接口不会报错（幂等性）
     *
     * @param strategyId 攻略ID（路径参数）
     * @return Result<Void> 取消成功返回空数据
     */
    @Operation(summary = "取消投票")
    @DeleteMapping
    public Result<Void> cancelVote(
            @Parameter(description = "玩法ID") @PathVariable Long strategyId) {

        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        strategyService.cancelVote(strategyId, userId);
        return Result.success();
    }

    /**
     * 获取当前登录用户ID —— 从 Spring Security 上下文中提取用户身份
     *
     * ═══════════════════════════════════════════════════════════════════
     * 工作原理
     * ═══════════════════════════════════════════════════════════════════
     *
     * 1. 用户登录时，JwtAuthenticationFilter 解析 JWT Token
     * 2. 将用户ID作为 Principal 存入 SecurityContext
     * 3. 本方法从 SecurityContext 取出 Principal（即用户ID字符串）
     * 4. 将字符串转换为 Long 类型的用户ID
     *
     * ═══════════════════════════════════════════════════════════════════
     * 异常处理
     * ═══════════════════════════════════════════════════════════════════
     *
     * - 如果 SecurityContext 中没有认证信息（未登录），返回 null
     * - 如果 Principal 无法转换为 Long（格式错误），返回 null
     * - 调用方需要检查返回值是否为 null，并返回 401 错误
     *
     * @return 用户ID（Long），如果未登录或提取失败返回 null
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            try {
                return Long.parseLong(authentication.getPrincipal().toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}