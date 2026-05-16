package com.aram.mayhem.controller;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.dto.HeroDetailVO;
import com.aram.mayhem.dto.HeroListVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.service.HeroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 英雄管理控制器
 *
 * 路径前缀：/api/heroes
 * 权限：公开访问
 * 功能：英雄列表分页查询、英雄详情查看
 * 关联：HeroService, HeroListVO, HeroDetailVO
 */
@Tag(name = "Hero", description = "英雄管理接口")
@RestController
@RequestMapping("/api/heroes")
public class HeroController {

    private final HeroService heroService;

    public HeroController(HeroService heroService) {
        this.heroService = heroService;
    }

    /**
     * 获取英雄列表（英雄模块）
     *
     * 作用：分页查询英雄列表，支持关键词搜索、梯级筛选和多种排序方式
     * 梯级等级：S+/S/A/B/C（从强到弱）
     * 排序字段：winRate(胜率)/pickRate(选取率)/tier(梯级)/name(名称)
     *
     * @param page    页码（从1开始）
     * @param size    每页数量
     * @param keyword 搜索关键词（英雄名/称号）
     * @param tier    梯级筛选（S+/S/A/B/C）
     * @param sortBy  排序字段
     * @return Result<PageResult<HeroListVO>> 分页英雄列表
     */
    @Operation(summary = "获取英雄列表", description = "分页查询英雄列表，支持关键词搜索、梯级筛选和排序")
    @GetMapping
    public Result<PageResult<HeroListVO>> getHeroList(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索关键词（英雄名/称号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "梯级筛选（S+/S/A/B/C）") @RequestParam(required = false) String tier,
            @Parameter(description = "排序字段（winRate/pickRate/tier/name）") @RequestParam(required = false) String sortBy) {
        // 调用Service层获取英雄列表（支持多条件筛选）
        PageResult<HeroListVO> result = heroService.getHeroList(page, size, keyword, tier, sortBy);
        return Result.success(result);
    }

    /**
     * 获取英雄详情（英雄模块）
     *
     * 作用：根据英雄ID获取详细信息，包括技能、属性、克制关系、推荐出装等
     *
     * @param id 英雄ID
     * @return Result<HeroDetailVO> 英雄详情
     */
    @Operation(summary = "获取英雄详情", description = "根据英雄ID获取详细信息")
    @GetMapping("/{id}")
    public Result<HeroDetailVO> getHeroDetail(
            @Parameter(description = "英雄ID") @PathVariable Long id) {
        // 调用Service层获取英雄详情
        HeroDetailVO detail = heroService.getHeroDetail(id);
        return Result.success(detail);
    }
}
