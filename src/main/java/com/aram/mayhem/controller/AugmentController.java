package com.aram.mayhem.controller;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.dto.AugmentRecommendRequest;
import com.aram.mayhem.dto.AugmentRecommendResponse;
import com.aram.mayhem.dto.AugmentListVO;
import com.aram.mayhem.dto.AugmentVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.dto.SynergyProgressResponse;
import com.aram.mayhem.service.AugmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 强化符文控制器
 *
 * 路径前缀：/api/augments
 * 权限：公开访问
 * 功能：符文列表查询、符文详情、套装进度、智能推荐
 * 关联：AugmentService, AugmentListVO, AugmentVO, SynergyProgressResponse, AugmentRecommendResponse
 */
@Tag(name = "强化符文", description = "强化符文查询接口")
@RestController
@RequestMapping("/api/augments")
public class AugmentController {

    private final AugmentService augmentService;

    public AugmentController(AugmentService augmentService) {
        this.augmentService = augmentService;
    }

    @Operation(summary = "获取强化符文列表", description = "支持按品质和套装筛选")
    @GetMapping
    public Result<PageResult<AugmentListVO>> getAugmentList(
            @Parameter(description = "品质：prismatic/legendary/epic/silver") @RequestParam(required = false) String quality,
            @Parameter(description = "套装：shield/regeneration/shield-break/... ") @RequestParam(required = false) String synergySet,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResult<AugmentListVO> result = augmentService.getAugmentList(page, size, quality, synergySet);
        return Result.success(result);
    }

    @Operation(summary = "获取强化符文详情", description = "根据ID获取强化符文详细信息")
    @GetMapping("/{id}")
    public Result<AugmentVO> getAugmentDetail(
            @Parameter(description = "强化符文ID") @PathVariable Long id) {

        AugmentVO augment = augmentService.getAugmentDetail(id);
        if (augment == null) {
            return Result.error(404, "强化符文不存在");
        }
        return Result.success(augment);
    }

    @Operation(summary = "获取套装进度", description = "根据已选符文ID列表获取各套装激活进度")
    @GetMapping("/synergy-progress")
    public Result<List<SynergyProgressResponse>> getSynergyProgress(
            @Parameter(description = "已选符文ID列表，用逗号分隔") @RequestParam String augmentIds) {

        List<SynergyProgressResponse> progress = augmentService.getSynergyProgress(augmentIds);
        return Result.success(progress);
    }

    @Operation(summary = "获取符文推荐", description = "根据英雄和已选符文推荐合适的强化符文")
    @PostMapping("/recommend")
    public Result<List<AugmentRecommendResponse>> getRecommendations(
            @RequestBody AugmentRecommendRequest request) {

        List<AugmentRecommendResponse> recommendations = augmentService.getRecommendations(request);
        return Result.success(recommendations);
    }
}