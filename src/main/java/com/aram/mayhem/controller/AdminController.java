package com.aram.mayhem.controller;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.dto.SyncResult;
import com.aram.mayhem.dto.TrapMarkRequest;
import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.mapper.AugmentMapper;
import com.aram.mayhem.mapper.HeroMapper;
import com.aram.mayhem.scheduler.DataSyncScheduler;
import com.aram.mayhem.service.CacheWarmupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 管理后台控制器 —— 系统管理员的"控制面板"，提供数据管理和运维操作接口
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类是管理后台的 REST API 入口，提供只有管理员才能使用的特殊操作：
 * 1. markHeroTrap()      → 标记/取消英雄的版本陷阱状态
 * 2. markAugmentTrap()   → 标记/取消符文的版本陷阱状态
 * 3. triggerSync()       → 手动触发全量数据同步
 * 4. triggerCacheWarmup()→ 手动触发缓存预热
 *
 * 与其他 Controller 的区别：
 * - 其他 Controller 的接口是给普通用户用的（公开或需登录）
 * - 本 Controller 的接口只给管理员使用（需要 ADMIN 角色）
 * - 类级别的 @PreAuthorize("hasRole('ADMIN')") 确保所有接口都需要管理员权限
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、权限控制机制
 * ═══════════════════════════════════════════════════════════════════
 *
 * @PreAuthorize("hasRole('ADMIN')") 是 Spring Security 的方法级权限控制注解：
 * - 在方法执行前检查当前用户是否拥有 ADMIN 角色
 * - 如果不是管理员，直接返回 403 Forbidden
 * - 放在类上表示该类所有方法都需要 ADMIN 权限
 *
 * 权限检查流程：
 * 1. 请求到达 → JwtAuthenticationFilter 解析 Token 获取用户信息
 * 2. Spring Security 检查用户角色 → 如果不是 ADMIN → 返回 403
 * 3. 如果是 ADMIN → 执行方法逻辑
 *
 * 注意：@EnableMethodSecurity 必须在 SecurityConfig 中启用，@PreAuthorize 才会生效。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、API 接口一览
 * ═══════════════════════════════════════════════════════════════════
 *
 * 方法                    | HTTP方法 | 路径                              | 权限    | 说明
 * ------------------------|---------|----------------------------------|--------|------------------
 * markHeroTrap()          | PUT     | /api/admin/heroes/{id}/trap-mark | ADMIN  | 标记英雄版本陷阱
 * markAugmentTrap()       | PUT     | /api/admin/augments/{id}/trap-mark| ADMIN  | 标记符文版本陷阱
 * triggerSync()           | POST    | /api/admin/sync/trigger          | ADMIN  | 手动触发数据同步
 * triggerCacheWarmup()    | POST    | /api/admin/cache/warmup          | ADMIN  | 手动触发缓存预热
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、版本陷阱（Version Trap）概念说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * "版本陷阱"是指当前版本被大幅削弱但玩家可能还根据旧印象选择的英雄/符文。
 * 就像游戏版本更新后，某个英雄被砍了一刀，但很多玩家不知道，
 * 还在选这个英雄，结果输得很惨——这就是"版本陷阱"。
 *
 * 管理员标记版本陷阱后：
 * - 前端会在该英雄/符文的卡片上显示红色警告横幅
 * - 推荐算法会降低陷阱英雄/符文的推荐权重
 * - 帮助玩家避开当前版本的"坑"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、数据同步与缓存预热说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 【数据同步 triggerSync()】
 * 正常情况下，数据同步由 DataSyncScheduler 每6小时自动执行。
 * 但有时候管理员需要立即更新数据（如游戏刚发布了新版本），
 * 这时可以手动触发同步，流程与自动同步完全一致：
 * 采集 → 聚合 → 验证 → 写入 → 缓存预热
 *
 * 【缓存预热 triggerCacheWarmup()】
 * 缓存预热是将热点数据提前加载到 Redis 缓存中。
 * 正常情况下，缓存预热在数据同步后自动执行。
 * 但如果 Redis 重启导致缓存丢失，管理员可以手动触发预热，
 * 避免用户首次访问时需要等待数据库查询。
 *
 * 关联类：
 * @see com.aram.mayhem.config.SecurityConfig 安全配置（启用方法级权限控制）
 * @see com.aram.mayhem.scheduler.DataSyncScheduler 数据同步调度器
 * @see com.aram.mayhem.service.CacheWarmupService 缓存预热服务
 * @see com.aram.mayhem.dto.TrapMarkRequest 陷阱标记请求对象
 * @see com.aram.mayhem.dto.SyncResult 数据同步结果
 */
@Tag(name = "Admin", description = "管理后台接口（需管理员权限）")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final HeroMapper heroMapper;
    private final AugmentMapper augmentMapper;
    private final DataSyncScheduler dataSyncScheduler;
    private final CacheWarmupService cacheWarmupService;

    @Value("${sync.warmup-top-n:50}")
    private int warmupTopN;

    /**
     * 构造函数 —— 注入所有依赖
     *
     * @param heroMapper          英雄数据库操作接口（直接操作数据库，不经过 Service 层）
     * @param augmentMapper       符文数据库操作接口
     * @param dataSyncScheduler   数据同步调度器（执行全量同步任务）
     * @param cacheWarmupService  缓存预热服务（将热点数据加载到 Redis）
     */
    public AdminController(HeroMapper heroMapper, AugmentMapper augmentMapper,
                           DataSyncScheduler dataSyncScheduler, CacheWarmupService cacheWarmupService) {
        this.heroMapper = heroMapper;
        this.augmentMapper = augmentMapper;
        this.dataSyncScheduler = dataSyncScheduler;
        this.cacheWarmupService = cacheWarmupService;
    }

    /**
     * 标记/取消英雄版本陷阱 —— 管理员手动标记英雄是否为版本陷阱
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：PUT（更新资源用 PUT）
     * 路径：/api/admin/heroes/{id}/trap-mark
     * 权限：仅管理员（@PreAuthorize hasRole('ADMIN')）
     * Content-Type：application/json
     *
     * ═══════════════════════════════════════════════════════════════════
     * 操作逻辑
     * ═══════════════════════════════════════════════════════════════════
     *
     * 标记（isVersionTrap=true）：
     * 1. 将英雄的 isVersionTrap 字段设为 true
     * 2. 将 versionTrapSince 字段设为当前时间（记录何时被标记为陷阱）
     * 3. 前端会显示红色警告横幅
     *
     * 取消标记（isVersionTrap=false）：
     * 1. 将英雄的 isVersionTrap 字段设为 false
     * 2. 将 versionTrapSince 字段设为 null（清除标记时间）
     * 3. 前端恢复正常显示
     *
     * ═══════════════════════════════════════════════════════════════════
     * 请求体示例
     * ═══════════════════════════════════════════════════════════════════
     *
     * 标记：{ "isVersionTrap": true }
     * 取消：{ "isVersionTrap": false }
     *
     * @param id      英雄ID（URL 路径参数）
     * @param request 陷阱标记请求体，包含 isVersionTrap 字段
     * @return Result<Void> 操作成功返回空数据，英雄不存在返回 404
     */
    @Operation(summary = "标记/取消英雄版本陷阱", description = "管理员标记英雄为版本陷阱，标记后前端显示红色警告横幅")
    @PutMapping("/heroes/{id}/trap-mark")
    public Result<Void> markHeroTrap(
            @Parameter(description = "英雄ID") @PathVariable Long id,
            @RequestBody TrapMarkRequest request) {
        log.info("Admin marking hero trap: heroId={}, isVersionTrap={}", id, request.getIsVersionTrap());

        Hero hero = heroMapper.selectById(id);
        if (hero == null) {
            return Result.error(404, "Hero not found with id: " + id);
        }

        hero.setIsVersionTrap(request.getIsVersionTrap());
        hero.setVersionTrapSince(request.getIsVersionTrap() ? LocalDateTime.now() : null);
        heroMapper.updateById(hero);

        log.info("Hero trap mark updated: heroId={}, isVersionTrap={}", id, request.getIsVersionTrap());
        return Result.success();
    }

    /**
     * 标记/取消强化符文版本陷阱 —— 管理员手动标记符文是否为版本陷阱
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：PUT
     * 路径：/api/admin/augments/{id}/trap-mark
     * 权限：仅管理员
     *
     * ═══════════════════════════════════════════════════════════════════
     * 操作逻辑
     * ═══════════════════════════════════════════════════════════════════
     *
     * 与 markHeroTrap 逻辑完全一致，只是操作对象从英雄变为符文：
     * - 标记：isVersionTrap=true, versionTrapSince=当前时间
     * - 取消：isVersionTrap=false, versionTrapSince=null
     *
     * @param id      强化符文ID（URL 路径参数）
     * @param request 陷阱标记请求体
     * @return Result<Void> 操作成功返回空数据，符文不存在返回 404
     */
    @Operation(summary = "标记/取消强化符文版本陷阱", description = "管理员标记强化符文为版本陷阱")
    @PutMapping("/augments/{id}/trap-mark")
    public Result<Void> markAugmentTrap(
            @PathVariable Long id,
            @RequestBody TrapMarkRequest request) {
        log.info("Admin marking augment trap: augmentId={}, isVersionTrap={}", id, request.getIsVersionTrap());

        Augment augment = augmentMapper.selectById(id);
        if (augment == null) {
            return Result.error(404, "Augment not found with id: " + id);
        }

        augment.setIsVersionTrap(request.getIsVersionTrap());
        augment.setVersionTrapSince(request.getIsVersionTrap() ? LocalDateTime.now() : null);
        augmentMapper.updateById(augment);

        log.info("Augment trap mark updated: augmentId={}, isVersionTrap={}", id, request.getIsVersionTrap());
        return Result.success();
    }

    /**
     * 手动触发数据同步 —— 管理员立即执行全量数据同步
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：POST（触发操作用 POST）
     * 路径：/api/admin/sync/trigger
     * 权限：仅管理员
     *
     * ═══════════════════════════════════════════════════════════════════
     * 同步流程
     * ═══════════════════════════════════════════════════════════════════
     *
     * 1. 获取分布式锁（防止多台服务器同时同步）
     * 2. 从 Riot DataDragon 采集英雄基础数据
     * 3. 从 U.GG 采集 ARAM 统计数据
     * 4. 多源数据聚合（合并 Riot + U.GG 数据）
     * 5. 数据验证（检查多源数据一致性）
     * 6. 写入数据库（插入新记录 / 更新已有记录）
     * 7. 缓存预热（将热点数据加载到 Redis）
     * 8. 释放分布式锁
     *
     * ═══════════════════════════════════════════════════════════════════
     * 使用场景
     * ═══════════════════════════════════════════════════════════════════
     *
     * - 游戏刚发布新版本，需要立即更新英雄数据
     * - 数据异常，需要重新同步修复
     * - 首次部署后，需要初始化数据
     *
     * @return Result<SyncResult> 同步结果，包含成功/失败状态和详细统计信息
     */
    @Operation(summary = "手动触发数据同步", description = "管理员手动触发全量数据同步（采集→聚合→验证→写入→缓存预热）")
    @PostMapping("/sync/trigger")
    public Result<SyncResult> triggerSync() {
        log.info("Admin triggered manual data sync");
        SyncResult result = dataSyncScheduler.syncAllData();
        if (result.isSuccess()) {
            return Result.success(result);
        } else {
            return Result.error(500, result.getErrorMessage());
        }
    }

    /**
     * 手动触发缓存预热 —— 管理员立即将热点数据加载到 Redis
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：POST
     * 路径：/api/admin/cache/warmup
     * 权限：仅管理员
     *
     * ═══════════════════════════════════════════════════════════════════
     * 预热内容
     * ═══════════════════════════════════════════════════════════════════
     *
     * 1. heroDetail 缓存：预热 Top N 英雄的详情数据（N 由配置决定，默认50）
     * 2. augmentList 缓存：预热全部符文列表数据
     * 3. heroList 缓存：预热英雄列表数据
     * 4. 清理过期缓存：删除 Redis 中不再使用的旧缓存键
     *
     * ═══════════════════════════════════════════════════════════════════
     * 使用场景
     * ═══════════════════════════════════════════════════════════════════
     *
     * - Redis 重启后缓存全部丢失，需要重新预热
     * - 缓存数据过期后，用户首次访问会变慢，提前预热避免此问题
     * - 数据同步后，主动更新缓存确保数据一致性
     *
     * @return Result<String> 预热结果摘要，包含各类缓存的预热数量
     */
    @Operation(summary = "手动触发缓存预热", description = "管理员手动触发缓存预热（英雄详情+符文列表+英雄列表+清理旧缓存）")
    @PostMapping("/cache/warmup")
    public Result<String> triggerCacheWarmup() {
        log.info("Admin triggered manual cache warmup");
        var warmedHeroes = cacheWarmupService.warmupHeroCache(warmupTopN);
        int warmedAugments = cacheWarmupService.warmupAugmentCache();
        int warmedHeroList = cacheWarmupService.warmupHeroListCache();
        int cleaned = cacheWarmupService.cleanupStaleCache();

        String summary = String.format(
                "HeroDetail: %d heroes warmed | AugmentList: %d augments warmed | HeroList: %d heroes warmed | StaleCleaned: %d keys",
                warmedHeroes.size(), warmedAugments, warmedHeroList, cleaned);
        log.info("Cache warmup completed: {}", summary);
        return Result.success(summary);
    }
}
