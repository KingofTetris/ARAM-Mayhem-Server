package com.aram.mayhem.controller;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.dto.BulletinDetailVO;
import com.aram.mayhem.dto.BulletinListVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.service.BulletinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告控制器
 *
 * 路径前缀：/api/bulletins
 * 权限：公开访问（SecurityConfig 配置 permitAll）
 * 功能：公告列表分页查询、最新公告获取、公告详情查看
 * 关联：BulletinService, BulletinListVO, BulletinDetailVO
 */
@Tag(name = "Bulletin", description = "公告管理接口")
@RestController
@RequestMapping("/api/bulletins")
public class BulletinController {

    private final BulletinService bulletinService;

    public BulletinController(BulletinService bulletinService) {
        this.bulletinService = bulletinService;
    }

    @Operation(summary = "获取公告列表", description = "分页查询公告列表，支持按类型筛选，置顶优先排序")
    @GetMapping
    public Result<PageResult<BulletinListVO>> getBulletinList(
            @Parameter(description = "公告类型（version/event/notice）") @RequestParam(required = false) String type,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        PageResult<BulletinListVO> result = bulletinService.getBulletinList(type, page, size);
        return Result.success(result);
    }

    @Operation(summary = "获取最新公告", description = "获取最新N条公告，用于首页轮播")
    @GetMapping("/latest")
    public Result<List<BulletinListVO>> getLatestBulletins(
            @Parameter(description = "获取数量") @RequestParam(defaultValue = "3") int limit) {
        List<BulletinListVO> result = bulletinService.getLatestBulletins(limit);
        return Result.success(result);
    }

    @Operation(summary = "获取公告详情", description = "根据公告ID获取详细信息")
    @GetMapping("/{id}")
    public Result<BulletinDetailVO> getBulletinDetail(
            @Parameter(description = "公告ID") @PathVariable Long id) {
        BulletinDetailVO detail = bulletinService.getBulletinDetail(id);
        return Result.success(detail);
    }
}
