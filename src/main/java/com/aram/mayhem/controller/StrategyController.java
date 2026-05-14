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

@Tag(name = "玩法", description = "玩法查询与发布接口")
@RestController
@RequestMapping("/api/strategies")
public class StrategyController {

    private final StrategyService strategyService;

    public StrategyController(StrategyService strategyService) {
        this.strategyService = strategyService;
    }

    @Operation(summary = "获取玩法列表", description = "支持 hot（按点赞差）和 latest（按时间）排序")
    @GetMapping
    public Result<PageResult<StrategyListVO>> getStrategyList(
            @Parameter(description = "排序方式：hot 或 latest") @RequestParam(defaultValue = "hot") String sort,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {

        List<StrategyListVO> strategies = strategyService.getStrategyList(sort, page, size);
        PageResult<StrategyListVO> result = new PageResult<>(strategies.size(), page, size, strategies);
        return Result.success(result);
    }

    @Operation(summary = "获取玩法详情")
    @GetMapping("/{id}")
    public Result<StrategyDetailVO> getStrategyDetail(
            @Parameter(description = "玩法ID") @PathVariable Long id) {

        StrategyDetailVO strategy = strategyService.getStrategyDetail(id);
        if (strategy == null) {
            return Result.error(404, "玩法不存在");
        }
        return Result.success(strategy);
    }

    @Operation(summary = "发布玩法", description = "需要登录")
    @PostMapping
    public Result<StrategyDetailVO> createStrategy(@Valid @RequestBody CreateStrategyRequest request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        try {
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
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "获取我的投稿")
    @GetMapping("/my")
    public Result<List<StrategyListVO>> getMyStrategies() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        List<StrategyListVO> strategies = strategyService.getUserStrategies(userId);
        return Result.success(strategies);
    }

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