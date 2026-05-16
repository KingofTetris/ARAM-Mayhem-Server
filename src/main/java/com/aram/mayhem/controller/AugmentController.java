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

    /**
     * 获取强化符文列表（符文模块）
     *
     * 作用：分页查询强化符文列表，支持按品质和套装筛选
     * 品质类型：prismatic(棱彩)/legendary(金色)/epic(史诗)/silver(银色)
     *
     * @param quality    品质筛选（可选）
     * @param synergySet 套装筛选（可选）
     * @param page       页码（从1开始）
     * @param size       每页数量
     * @return Result<PageResult<AugmentListVO>> 分页符文列表
     */
    @Operation(summary = "获取强化符文列表", description = "支持按品质和套装筛选")
    @GetMapping
    public Result<PageResult<AugmentListVO>> getAugmentList(
            @Parameter(description = "品质：prismatic/legendary/epic/silver") @RequestParam(required = false) String quality,
            @Parameter(description = "套装：shield/regeneration/shield-break/... ") @RequestParam(required = false) String synergySet,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        // 调用Service层获取符文列表（支持筛选条件）
        PageResult<AugmentListVO> result = augmentService.getAugmentList(page, size, quality, synergySet);
        return Result.success(result);
    }

    /**
     * 获取强化符文详情（符文模块）
     *
     * 作用：根据符文ID获取详细信息，包括效果描述、品质、套装属性等
     *
     * @param id 强化符文ID
     * @return Result<AugmentVO> 符文详情，不存在返回404错误
     */
    @Operation(summary = "获取强化符文详情", description = "根据ID获取强化符文详细信息")
    @GetMapping("/{id}")
    public Result<AugmentVO> getAugmentDetail(
            @Parameter(description = "强化符文ID") @PathVariable Long id) {

        // 调用Service层获取符文详情
        AugmentVO augment = augmentService.getAugmentDetail(id);
        if (augment == null) {
            return Result.error(404, "强化符文不存在");
        }
        return Result.success(augment);
    }

    /**
     * 获取套装进度（符文模块）
     *
     * 作用：根据已选符文ID列表，计算并返回各套装的激活进度
     * 套装类型：shield(护盾)/regeneration(再生)/shield-break(破盾)等
     *
     * @param augmentIds 已选符文ID列表（逗号分隔）
     * @return Result<List<SynergyProgressResponse>> 各套装进度列表
     */
    @Operation(summary = "获取套装进度", description = "根据已选符文ID列表获取各套装激活进度")
    @GetMapping("/synergy-progress")
    public Result<List<SynergyProgressResponse>> getSynergyProgress(
            @Parameter(description = "已选符文ID列表，用逗号分隔") @RequestParam String augmentIds) {

        // 调用Service层计算套装进度
        List<SynergyProgressResponse> progress = augmentService.getSynergyProgress(augmentIds);
        return Result.success(progress);
    }

    /**
     * 获取符文推荐（符文模块）
     *
     * 作用：根据用户选择的英雄和已选符文，智能推荐合适的强化符文
     * 推荐算法：胜率权重 + 套装协同权重 - 陷阱惩罚
     *
     * @param request 推荐请求体（英雄ID、已选符文ID列表）
     * @return Result<List<AugmentRecommendResponse>> 符文推荐列表（按评分排序）
     */
    @Operation(summary = "获取符文推荐", description = "根据英雄和已选符文推荐合适的强化符文")
    @PostMapping("/recommend")
    public Result<List<AugmentRecommendResponse>> getRecommendations(
            @RequestBody AugmentRecommendRequest request) {

        // 调用Service层执行智能推荐算法
        List<AugmentRecommendResponse> recommendations = augmentService.getRecommendations(request);
        return Result.success(recommendations);
    }
}