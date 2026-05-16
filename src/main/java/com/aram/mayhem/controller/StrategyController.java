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
 * 攻略控制器
 *
 * 路径前缀：/api/strategies
 * 权限：列表/详情公开访问，创建需登录
 * 功能：攻略列表分页查询、攻略详情、创建攻略
 * 关联：StrategyService, StrategyListVO, StrategyDetailVO, CreateStrategyRequest
 */
@Tag(name = "Strategy", description = "攻略管理接口")
@RestController
@RequestMapping("/api/strategies")
public class StrategyController {

    private final StrategyService strategyService;

    public StrategyController(StrategyService strategyService) {
        this.strategyService = strategyService;
    }

    /**
     * 获取玩法列表（社区模块）
     *
     * 作用：分页查询攻略列表，支持按热度或时间排序
     * 排序规则：hot-按点赞差排序，latest-按发布时间排序
     *
     * @param sort 排序方式（hot-热门/最新）
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return Result<PageResult<StrategyListVO>> 分页攻略列表
     */
    @Operation(summary = "获取玩法列表", description = "支持 hot（按点赞差）和 latest（按时间）排序")
    @GetMapping
    public Result<PageResult<StrategyListVO>> getStrategyList(
            @Parameter(description = "排序方式：hot 或 latest") @RequestParam(defaultValue = "hot") String sort,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {

        // 调用Service层获取攻略列表
        List<StrategyListVO> strategies = strategyService.getStrategyList(sort, page, size);
        // 封装分页结果
        PageResult<StrategyListVO> result = new PageResult<>(strategies.size(), page, size, strategies);
        return Result.success(result);
    }

    /**
     * 获取玩法详情（社区模块）
     *
     * 作用：根据玩法ID获取详细信息，包括关联的英雄、符文、装备等
     *
     * @param id 玩法ID
     * @return Result<StrategyDetailVO> 玩法详情，不存在返回404错误
     */
    @Operation(summary = "获取玩法详情")
    @GetMapping("/{id}")
    public Result<StrategyDetailVO> getStrategyDetail(
            @Parameter(description = "玩法ID") @PathVariable Long id) {

        // 调用Service层获取详情
        StrategyDetailVO strategy = strategyService.getStrategyDetail(id);
        if (strategy == null) {
            return Result.error(404, "玩法不存在");
        }
        return Result.success(strategy);
    }

    /**
     * 发布玩法（社区模块）
     *
     * 作用：用户发布新的游戏攻略，包含英雄、标题、描述、符文和装备关联
     * 权限：需要登录
     * 事务：创建攻略需同时写入攻略主体+符文关联+装备关联
     *
     * @param request 发布攻略请求体
     * @return Result<StrategyDetailVO> 发布后的攻略详情
     */
    @Operation(summary = "发布玩法", description = "需要登录")
    @PostMapping
    public Result<StrategyDetailVO> createStrategy(@Valid @RequestBody CreateStrategyRequest request) {
        // 获取当前登录用户ID
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        try {
            // 调用Service层创建攻略（含事务管理）
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
            // 参数校验失败或业务规则不满足
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 获取我的投稿（社区模块）
     *
     * 作用：获取当前登录用户发布的所有攻略列表
     * 权限：需要登录
     *
     * @return Result<List<StrategyListVO>> 用户发布的攻略列表
     */
    @Operation(summary = "获取我的投稿")
    @GetMapping("/my")
    public Result<List<StrategyListVO>> getMyStrategies() {
        // 获取当前登录用户ID
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        // 调用Service层获取用户攻略
        List<StrategyListVO> strategies = strategyService.getUserStrategies(userId);
        return Result.success(strategies);
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
                return Long.parseLong(authentication.getPrincipal().toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}