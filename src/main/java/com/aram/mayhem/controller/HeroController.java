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
 * 英雄管理控制器 —— 英雄模块的"前台接待处"，处理英雄相关的 HTTP 请求
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类是英雄模块的 REST API 入口，提供两个核心接口：
 * 1. getHeroList()  → 分页查询英雄列表（支持搜索、筛选、排序）
 * 2. getHeroDetail() → 查询单个英雄的详细信息
 *
 * Controller 层只负责"接待"：接收请求、校验参数、调用 Service、包装响应。
 * 实际的业务逻辑（数据库查询、缓存、数据转换）由 HeroService 负责。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、API 接口一览
 * ═══════════════════════════════════════════════════════════════════
 *
 * 方法            | HTTP方法 | 路径              | 权限  | 说明
 * ---------------|---------|-------------------|------|------------------
 * getHeroList()  | GET     | /api/heroes       | 公开  | 分页查询英雄列表
 * getHeroDetail()| GET     | /api/heroes/{id}  | 公开  | 查询英雄详情
 *
 * 两个接口都是"公开访问"，不需要 JWT Token。
 * 在 SecurityConfig 中，/api/heroes/** 路径被配置为 permitAll()。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、前端使用场景
 * ═══════════════════════════════════════════════════════════════════
 *
 * - 英雄列表页：调用 getHeroList() 获取分页数据，支持搜索和筛选
 * - 英雄详情页：点击某个英雄后，调用 getHeroDetail() 获取完整信息
 * - 首页推荐：调用 getHeroList(sortBy=winRate) 获取胜率最高的英雄
 *
 * 关联类：
 * @see com.aram.mayhem.service.HeroService 英雄服务（实际业务逻辑）
 * @see com.aram.mayhem.common.Result 统一响应包装类
 * @see com.aram.mayhem.dto.HeroListVO 英雄列表项视图对象
 * @see com.aram.mayhem.dto.HeroDetailVO 英雄详情视图对象
 * @see com.aram.mayhem.dto.PageResult 分页结果包装类
 */
@Tag(name = "Hero", description = "英雄管理接口")
@RestController
@RequestMapping("/api/heroes")
public class HeroController {

    private final HeroService heroService;

    /**
     * 构造函数 —— 注入英雄服务
     *
     * @param heroService 英雄服务，负责英雄列表查询和详情查询的业务逻辑
     */
    public HeroController(HeroService heroService) {
        this.heroService = heroService;
    }

    /**
     * 获取英雄列表接口 —— 分页查询，支持搜索、筛选和排序
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：GET（查询资源用 GET）
     * 路径：/api/heroes
     * 权限：公开（不需要 Token）
     *
     * ═══════════════════════════════════════════════════════════════════
     * 请求参数说明
     * ═══════════════════════════════════════════════════════════════════
     *
     * 参数      | 类型   | 必填 | 默认值 | 说明
     * ----------|-------|------|-------|----------------------------------
     * page      | int   | 否   | 1     | 页码，从1开始
     * size      | int   | 否   | 10    | 每页数量
     * keyword   | String| 否   | null  | 搜索关键词（匹配英雄名/称号）
     * tier      | String| 否   | null  | 梯级筛选（S+/S/A/B/C）
     * sortBy    | String| 否   | null  | 排序字段（winRate/pickRate/tier/name）
     *
     * ═══════════════════════════════════════════════════════════════════
     * 请求示例
     * ═══════════════════════════════════════════════════════════════════
     *
     * GET /api/heroes?page=1&size=10&keyword=盖伦&tier=S&sortBy=winRate
     * GET /api/heroes（使用所有默认值）
     *
     * @param page    页码（从1开始），默认1
     * @param size    每页数量，默认10
     * @param keyword 搜索关键词（英雄名/称号），可选
     * @param tier    梯级筛选（S+/S/A/B/C），可选
     * @param sortBy  排序字段（winRate/pickRate/tier/name），可选
     * @return Result<PageResult<HeroListVO>> 分页英雄列表，包含总记录数和当前页数据
     */
    @Operation(summary = "获取英雄列表", description = "分页查询英雄列表，支持关键词搜索、梯级筛选和排序")
    @GetMapping
    public Result<PageResult<HeroListVO>> getHeroList(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索关键词（英雄名/称号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "梯级筛选（S+/S/A/B/C）") @RequestParam(required = false) String tier,
            @Parameter(description = "排序字段（winRate/pickRate/tier/name）") @RequestParam(required = false) String sortBy) {
        // 调用 HeroService 获取英雄列表
        // HeroService 内部会使用 Spring Cache 缓存查询结果，减少数据库访问
        PageResult<HeroListVO> result = heroService.getHeroList(page, size, keyword, tier, sortBy);
        return Result.success(result);
    }

    /**
     * 获取英雄详情接口 —— 根据ID查询单个英雄的完整信息
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：GET
     * 路径：/api/heroes/{id}
     * 权限：公开（不需要 Token）
     *
     * ═══════════════════════════════════════════════════════════════════
     * 路径参数说明
     * ═══════════════════════════════════════════════════════════════════
     *
     * 参数 | 类型   | 必填 | 说明
     * -----|-------|------|------------------
     * id   | Long  | 是   | 英雄ID（数据库主键）
     *
     * ═══════════════════════════════════════════════════════════════════
     * 请求示例
     * ═══════════════════════════════════════════════════════════════════
     *
     * GET /api/heroes/1
     *
     * @param id 英雄ID，通过 @PathVariable 从 URL 路径中提取
     * @return Result<HeroDetailVO> 英雄详情，包含技能、属性、推荐符文等完整信息
     */
    @Operation(summary = "获取英雄详情", description = "根据英雄ID获取详细信息")
    @GetMapping("/{id}")
    public Result<HeroDetailVO> getHeroDetail(
            @Parameter(description = "英雄ID") @PathVariable Long id) {
        // 调用 HeroService 获取英雄详情
        // HeroService 内部会使用 Spring Cache 缓存详情数据
        HeroDetailVO detail = heroService.getHeroDetail(id);
        return Result.success(detail);
    }
}
