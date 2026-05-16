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
 * 投票控制器
 *
 * 路径前缀：/api/strategies/{id}/vote
 * 权限：需登录
 * 功能：对攻略进行点赞/踩投票
 * 关联：StrategyService, VoteRequest
 */
@Tag(name = "投票", description = "玩法投票接口")
@RestController
@RequestMapping("/api/strategies/{strategyId}/vote")
public class VoteController {

    private final StrategyService strategyService;

    public VoteController(StrategyService strategyService) {
        this.strategyService = strategyService;
    }

    /**
     * 对玩法进行投票（社区模块）
     *
     * 作用：用户对指定玩法进行点赞(UP)或点踩(DOWN)操作
     * 业务规则：同一用户对同一玩法只能投一票，重复投票会切换投票状态
     *
     * @param strategyId 玩法ID
     * @param request    投票请求体，包含投票类型(UP/DOWN)
     * @return Result<Void> 投票成功返回空数据，失败返回错误信息
     */
    @Operation(summary = "投票", description = "对玩法进行投票，UP 为点赞，DOWN 为点踩")
    @PostMapping
    public Result<Void> vote(
            @Parameter(description = "玩法ID") @PathVariable Long strategyId,
            @Valid @RequestBody VoteRequest request) {

        // 获取当前登录用户ID（从SecurityContext中提取）
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        try {
            // 调用Service层执行投票逻辑
            strategyService.vote(strategyId, userId, request.getVoteType());
            return Result.success();
        } catch (IllegalStateException e) {
            // 业务异常：如重复投票、投票状态错误等
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 取消投票（社区模块）
     *
     * 作用：取消用户对指定玩法的投票，恢复初始状态
     *
     * @param strategyId 玩法ID
     * @return Result<Void> 取消成功返回空数据，失败返回错误信息
     */
    @Operation(summary = "取消投票")
    @DeleteMapping
    public Result<Void> cancelVote(
            @Parameter(description = "玩法ID") @PathVariable Long strategyId) {

        // 获取当前登录用户ID
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        // 调用Service层取消投票
        strategyService.cancelVote(strategyId, userId);
        return Result.success();
    }

    /**
     * 获取当前登录用户ID
     *
     * 作用：从SecurityContext中提取当前认证用户的ID
     * 实现：Authentication.getPrincipal()存储的是用户ID字符串
     *
     * @return 用户ID，如果未登录或提取失败返回null
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            try {
                // 将Principal字符串转换为Long类型的用户ID
                return Long.parseLong(authentication.getPrincipal().toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}