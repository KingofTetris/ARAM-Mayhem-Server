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
 * 强化符文控制器 —— 符文模块的"前台接待处"，处理符文相关的 HTTP 请求
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类是强化符文模块的 REST API 入口，提供四个核心接口：
 * 1. getAugmentList()      → 分页查询符文列表（支持品质和套装筛选）
 * 2. getAugmentDetail()    → 查询单个符文详情
 * 3. getSynergyProgress()  → 计算已选符文的套装激活进度
 * 4. getRecommendations()  → 基于英雄和已选符文智能推荐
 *
 * Controller 层只负责"接待"：接收请求、校验参数、调用 Service、包装响应。
 * 实际的业务逻辑由 AugmentService 负责。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、API 接口一览
 * ═══════════════════════════════════════════════════════════════════
 *
 * 方法                  | HTTP方法 | 路径                           | 权限  | 说明
 * ---------------------|---------|-------------------------------|------|------------------
 * getAugmentList()     | GET     | /api/augments                 | 公开  | 符文列表查询
 * getAugmentDetail()   | GET     | /api/augments/{id}            | 公开  | 符文详情查询
 * getSynergyProgress() | GET     | /api/augments/synergy-progress| 公开  | 套装进度计算
 * getRecommendations() | POST    | /api/augments/recommend       | 公开  | 智能推荐
 *
 * 所有接口都是"公开访问"，不需要 JWT Token。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、强化符文系统简介
 * ═══════════════════════════════════════════════════════════════════
 *
 * ARAM 模式中的强化符文是游戏中的特殊增益效果：
 * - 品质（Quality）：prismatic(棱彩) > legendary(金色) > epic(史诗) > silver(银色)
 * - 套装（Synergy）：shield(护盾)、regeneration(再生)、shield-break(破盾) 等
 * - 集齐同一套装的多个符文，可以激活套装效果（类似装备套装加成）
 *
 * 关联类：
 * @see com.aram.mayhem.service.AugmentService 强化符文服务（实际业务逻辑）
 * @see com.aram.mayhem.common.Result 统一响应包装类
 * @see com.aram.mayhem.dto.AugmentListVO 符文列表项视图对象
 * @see com.aram.mayhem.dto.AugmentVO 符文详情视图对象
 * @see com.aram.mayhem.dto.SynergyProgressResponse 套装进度响应对象
 * @see com.aram.mayhem.dto.AugmentRecommendResponse 符文推荐响应对象
 * @see com.aram.mayhem.dto.AugmentRecommendRequest 符文推荐请求对象
 */
@Tag(name = "强化符文", description = "强化符文查询接口")
@RestController
@RequestMapping("/api/augments")
public class AugmentController {

    private final AugmentService augmentService;

    /**
     * 构造函数 —— 注入强化符文服务
     *
     * @param augmentService 强化符文服务，负责符文查询、套装计算和推荐的业务逻辑
     */
    public AugmentController(AugmentService augmentService) {
        this.augmentService = augmentService;
    }

    /**
     * 获取强化符文列表接口 —— 分页查询，支持品质和套装筛选
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：GET
     * 路径：/api/augments
     * 权限：公开
     *
     * ═══════════════════════════════════════════════════════════════════
     * 请求参数说明
     * ═══════════════════════════════════════════════════════════════════
     *
     * 参数        | 类型   | 必填 | 默认值 | 说明
     * ------------|-------|------|-------|----------------------------------------
     * quality     | String| 否   | null  | 品质筛选（prismatic/legendary/epic/silver）
     * synergySet  | String| 否   | null  | 套装筛选（shield/regeneration/...）
     * page        | int   | 否   | 1     | 页码，从1开始
     * size        | int   | 否   | 20    | 每页数量
     *
     * @param quality    品质筛选，可选值：prismatic/legendary/epic/silver
     * @param synergySet 套装筛选，可选值：shield/regeneration/shield-break/...
     * @param page       页码，从1开始
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

        // 调用 AugmentService 获取符文列表
        // AugmentService 内部会使用 Spring Cache 缓存查询结果
        PageResult<AugmentListVO> result = augmentService.getAugmentList(page, size, quality, synergySet);
        return Result.success(result);
    }

    /**
     * 获取强化符文详情接口 —— 根据ID查询单个符文的完整信息
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：GET
     * 路径：/api/augments/{id}
     * 权限：公开
     *
     * @param id 强化符文ID，通过 @PathVariable 从 URL 路径中提取
     * @return Result<AugmentVO> 符文详情，不存在返回 404 错误
     */
    @Operation(summary = "获取强化符文详情", description = "根据ID获取强化符文详细信息")
    @GetMapping("/{id}")
    public Result<AugmentVO> getAugmentDetail(
            @Parameter(description = "强化符文ID") @PathVariable Long id) {

        // 调用 AugmentService 获取符文详情
        AugmentVO augment = augmentService.getAugmentDetail(id);
        // 如果查询结果为 null，说明符文不存在，返回 404
        if (augment == null) {
            return Result.error(404, "强化符文不存在");
        }
        return Result.success(augment);
    }

    /**
     * 获取套装进度接口 —— 计算已选符文的套装激活进度
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：GET
     * 路径：/api/augments/synergy-progress
     * 权限：公开
     *
     * ═══════════════════════════════════════════════════════════════════
     * 使用场景
     * ═══════════════════════════════════════════════════════════════════
     *
     * 前端在符文选择页面，用户每选择一个符文后，
     * 调用此接口实时计算各套装的激活进度，
     * 用进度条展示"再选N个同套装符文即可激活套装效果"。
     *
     * @param augmentIds 已选符文ID列表，用逗号分隔（如 "1,2,3"）
     * @return Result<List<SynergyProgressResponse>> 各套装进度列表
     */
    @Operation(summary = "获取套装进度", description = "根据已选符文ID列表获取各套装激活进度")
    @GetMapping("/synergy-progress")
    public Result<List<SynergyProgressResponse>> getSynergyProgress(
            @Parameter(description = "已选符文ID列表，用逗号分隔") @RequestParam String augmentIds) {

        // 调用 AugmentService 计算套装进度
        // augmentIds 是逗号分隔的 ID 字符串，Service 层会解析为 List<Long>
        List<SynergyProgressResponse> progress = augmentService.getSynergyProgress(augmentIds);
        return Result.success(progress);
    }

    /**
     * 获取符文推荐接口 —— 根据英雄和已选符文智能推荐
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：POST（请求体较复杂，用 POST 比 GET 更合适）
     * 路径：/api/augments/recommend
     * 权限：公开
     *
     * ═══════════════════════════════════════════════════════════════════
     * 推荐算法说明
     * ═══════════════════════════════════════════════════════════════════
     *
     * 综合评分 = 胜率权重 × 胜率 + 套装协同权重 × 套装匹配度 - 陷阱惩罚
     * - 胜率权重：该符文在该英雄上的历史胜率
     * - 套装协同权重：该符文与已选符文的套装匹配程度
     * - 陷阱惩罚：低胜率符文的降权惩罚
     *
     * @param request 推荐请求体，包含：
     *                - heroId：当前选择的英雄ID
     *                - augmentIds：已选符文ID列表
     * @return Result<List<AugmentRecommendResponse>> 推荐列表，按评分降序排列
     */
    @Operation(summary = "获取符文推荐", description = "根据英雄和已选符文推荐合适的强化符文")
    @PostMapping("/recommend")
    public Result<List<AugmentRecommendResponse>> getRecommendations(
            @RequestBody AugmentRecommendRequest request) {

        // 调用 AugmentService 执行智能推荐算法
        List<AugmentRecommendResponse> recommendations = augmentService.getRecommendations(request);
        return Result.success(recommendations);
    }
}