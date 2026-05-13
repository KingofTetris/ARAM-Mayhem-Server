package com.aram.mayhem.controller;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.dto.AugmentListVO;
import com.aram.mayhem.dto.AugmentVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.service.AugmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

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
            @Parameter(description = "品质：prismatic/legendary/epic") @RequestParam(required = false) String quality,
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
}
