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

@Tag(name = "投票", description = "玩法投票接口")
@RestController
@RequestMapping("/api/strategies/{strategyId}/vote")
public class VoteController {

    private final StrategyService strategyService;

    public VoteController(StrategyService strategyService) {
        this.strategyService = strategyService;
    }

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