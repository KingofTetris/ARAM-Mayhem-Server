package com.aram.mayhem.scheduler;

import com.aram.mayhem.client.RiotDataDragonClient;
import com.aram.mayhem.dto.SyncResult;
import com.aram.mayhem.dto.ValidationResult;
import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.service.CacheWarmupService;
import com.aram.mayhem.service.DataAggregatorService;
import com.aram.mayhem.service.DistributedLockService;
import com.aram.mayhem.service.MultiSourceValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据同步调度器 —— 定时触发数据管线的"闹钟"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类负责定时触发数据同步任务，是整个数据管线的"启动按钮"。
 * 它使用 Spring 的 @Scheduled 注解，按照配置的 cron 表达式定时执行同步。
 *
 * 打个比方：
 * - 如果数据管线是一条生产线，那么本类就是"定时启动生产线的闹钟"
 * - RiotDataDragonClient 和 AramDataCollector 是"采购员"（去取原材料）
 * - DataAggregatorService 是"加工车间"（合并、清洗、存储数据）
 * - CacheWarmupService 是"仓库管理员"（把成品提前放到货架）
 * - DistributedLockService 是"门锁"（防止多台服务器同时启动生产线）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、同步流程
 * ═══════════════════════════════════════════════════════════════════
 *
 * syncAllData() 方法的完整执行流程：
 *
 * ┌──────────────────────────────────────────────────────────────┐
 * │ 第1步：尝试获取分布式锁                                      │
 * │   ↓ 获取失败 → 跳过本轮同步（另一台服务器正在同步）           │
 * │   ↓ 获取成功 → 继续                                         │
 * ├──────────────────────────────────────────────────────────────┤
 * │ 第2步：获取 DataDragon 最新版本号                            │
 * │   ↓ 失败 → 终止同步，释放锁                                 │
 * │   ↓ 成功 → 继续                                             │
 * ├──────────────────────────────────────────────────────────────┤
 * │ 第3步：聚合并保存英雄数据                                    │
 * │   → DataAggregatorService.aggregateAndSaveHeroData(version)  │
 * ├──────────────────────────────────────────────────────────────┤
 * │ 第4步：聚合并保存符文数据                                    │
 * │   → DataAggregatorService.aggregateAndSaveAugmentData()      │
 * ├──────────────────────────────────────────────────────────────┤
 * │ 第5步：验证数据质量                                          │
 * │   → MultiSourceValidator.validateHeroStats()                 │
 * │   → MultiSourceValidator.validateAugmentStats()              │
 * ├──────────────────────────────────────────────────────────────┤
 * │ 第6步：缓存预热                                              │
 * │   → CacheWarmupService.warmupHeroCache()                    │
 * │   → CacheWarmupService.warmupAugmentCache()                 │
 * │   → CacheWarmupService.warmupHeroListCache()                │
 * │   → CacheWarmupService.cleanupStaleCache()                  │
 * ├──────────────────────────────────────────────────────────────┤
 * │ 第7步：记录同步结果，释放分布式锁                            │
 * └──────────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、定时策略
 * ═══════════════════════════════════════════════════════════════════
 *
 * Cron 表达式：0 0 0/6 * * ?
 * 含义：每6小时执行一次（0:00, 6:00, 12:00, 18:00）
 * 为什么是6小时？
 * - Riot 游戏版本更新通常不会太频繁
 * - U.GG 的统计数据需要一定时间积累才准确
 * - 6小时是一个平衡点：不太频繁（避免浪费资源），也不太慢（数据不会太旧）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、分布式锁说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 锁的 Key：data:sync
 * 锁的 TTL：30 分钟（30 * 60 * 1000 毫秒）
 *
 * 为什么需要分布式锁？
 * - 如果部署了多台服务器，每台都会执行定时任务
 * - 不加锁的话，多台服务器会同时同步数据，导致：
 *   - 数据库重复写入
 *   - 缓存重复预热
 *   - 外部 API 被过度调用
 * - 加锁后，只有获得锁的服务器执行同步，其他服务器跳过
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、依赖说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 依赖对象                  | 作用                           | 打个比方
 * --------------------------|-------------------------------|------------------
 * DataAggregatorService     | 聚合多数据源并保存到数据库      | 加工车间
 * MultiSourceValidator      | 验证数据质量                   | 质检员
 * DistributedLockService    | 分布式锁                       | 门锁
 * RiotDataDragonClient      | 获取最新版本号                 | 采购员
 * CacheWarmupService        | 缓存预热和清理                 | 仓库管理员
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、关联类
 * ═══════════════════════════════════════════════════════════════════
 *
 * - RiotDataDragonClient → 获取版本号和英雄基础数据
 * - AramDataCollector → 采集 U.GG 统计数据
 * - DataAggregatorServiceImpl → 执行数据聚合和存储
 * - MultiSourceValidatorImpl → 验证数据质量
 * - DistributedLockServiceImpl → 提供分布式锁
 * - CacheWarmupServiceImpl → 执行缓存预热
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncScheduler {

    /**
     * 分布式锁的 Key
     *
     * 所有服务器使用同一个 Key，确保同一时刻只有一台服务器能获得锁
     */
    private static final String SYNC_LOCK_KEY = "data:sync";

    /**
     * 分布式锁的 TTL（生存时间）
     *
     * 30 分钟 = 30 * 60 * 1000 毫秒
     * 为什么是30分钟？
     * - 正常同步大约需要 1~5 分钟
     * - 30 分钟是安全上限，防止因异常导致锁永远不释放（死锁）
     * - 如果同步真的超过 30 分钟，锁会自动过期，其他服务器可以接管
     */
    private static final long LOCK_TTL_MS = 30 * 60 * 1000L;

    /**
     * 数据聚合服务 —— 负责从多个数据源采集、合并、清洗并存储数据
     */
    private final DataAggregatorService dataAggregatorService;

    /**
     * 多源数据验证器 —— 负责验证聚合后的数据质量
     */
    private final MultiSourceValidator multiSourceValidator;

    /**
     * 分布式锁服务 —— 负责获取和释放分布式锁
     */
    private final DistributedLockService distributedLockService;

    /**
     * Riot DataDragon 客户端 —— 获取最新版本号
     */
    private final RiotDataDragonClient riotDataDragonClient;

    /**
     * 缓存预热服务 —— 同步完成后预热缓存
     */
    private final CacheWarmupService cacheWarmupService;

    /**
     * 缓存预热时查询的 Top N 英雄数量
     *
     * 从 application.yml 读取，默认值为 50
     * 含义：预热胜率最高的 50 个英雄的详情缓存
     * 为什么只预热 Top N？因为英雄很多（160+），全部预热太慢且浪费内存
     */
    @Value("${sync.warmup-top-n:50}")
    private int warmupTopN;

    /**
     * 定时同步所有数据 —— 数据管线的入口方法
     *
     * ══════════════════════════════════════════════════════════════
     * 执行时机
     * ══════════════════════════════════════════════════════════════
     *
     * 由 @Scheduled(cron = "0 0 0/6 * * ?") 控制，每6小时执行一次
     * 也可以手动调用（如通过管理接口触发）
     *
     * ══════════════════════════════════════════════════════════════
     * 返回值说明
     * ══════════════════════════════════════════════════════════════
     *
     * 返回 SyncResult 对象，包含：
     * - 成功/失败标志
     * - 新增/更新的英雄和符文数量
     * - 数据验证通过/失败的数量
     * - 同步耗时（毫秒）
     *
     * @return 同步结果，包含详细的统计信息
     */
    @Scheduled(cron = "0 0 0/6 * * ?")
    public SyncResult syncAllData() {
        log.info("[SYNC] scheduled data sync started");
        long startTime = System.currentTimeMillis();

        // 第1步：尝试获取分布式锁
        // 如果获取失败，说明另一台服务器正在同步，本轮跳过
        if (!distributedLockService.tryLock(SYNC_LOCK_KEY, LOCK_TTL_MS)) {
            log.warn("[SYNC] another sync is running, skip this round");
            return SyncResult.fail("Another sync is already running");
        }

        try {
            // 第2步：获取 DataDragon 最新版本号
            // 版本号是后续数据聚合的必要参数
            String version = riotDataDragonClient.fetchLatestVersion();
            if (version == null || version.isBlank()) {
                log.error("[SYNC] failed to fetch latest version from DataDragon");
                return SyncResult.fail("Failed to fetch latest version");
            }
            log.info("[SYNC] using DataDragon version={}", version);

            // 第3步：聚合并保存英雄数据
            // 从 RiotDataDragon + U.GG 采集数据，合并后保存到 tb_hero 表
            List<Hero> heroes = dataAggregatorService.aggregateAndSaveHeroData(version);

            // 第4步：聚合并保存符文数据
            // 从 U.GG 采集符文统计数据，保存到 tb_augment 表
            List<Augment> augments = dataAggregatorService.aggregateAndSaveAugmentData();

            // 第5步：验证数据质量
            // 检查英雄和符文的统计数据是否合理（胜率范围、置信度等）
            List<ValidationResult> heroValidations = multiSourceValidator.validateHeroStats(heroes);
            List<ValidationResult> augmentValidations = multiSourceValidator.validateAugmentStats(augments);

            // 统计验证通过和失败的数量
            int validationPassed = (int) heroValidations.stream().filter(ValidationResult::isPassed).count()
                    + (int) augmentValidations.stream().filter(ValidationResult::isPassed).count();
            int validationFailed = heroValidations.size() + augmentValidations.size() - validationPassed;

            // 第6步：缓存预热
            // 同步完成后，提前把热点数据加载到 Redis 缓存
            cacheWarmupService.warmupHeroCache(warmupTopN);    // 预热 Top N 英雄详情
            cacheWarmupService.warmupAugmentCache();            // 预热全部符文列表
            cacheWarmupService.warmupHeroListCache();           // 预热英雄列表分页
            cacheWarmupService.cleanupStaleCache();             // 清理过期缓存
            log.info("[SYNC] cache warmup completed");

            // 第7步：记录同步结果
            long durationMs = System.currentTimeMillis() - startTime;
            log.info("[SYNC] completed | heroes={} | augments={} | passed={} | failed={} | duration={}ms",
                    heroes.size(), augments.size(), validationPassed, validationFailed, durationMs);

            return SyncResult.success(
                    countInserted(heroes), countUpdated(heroes),
                    countInserted(augments), countUpdated(augments),
                    validationPassed, validationFailed, durationMs);
        } catch (Exception e) {
            // 同步过程中出现未预期的异常，记录错误并返回失败结果
            log.error("[SYNC] failed with exception", e);
            return SyncResult.fail(e.getMessage());
        } finally {
            // 无论成功还是失败，都必须释放分布式锁
            // 如果不释放，其他服务器将永远无法执行同步（死锁）
            distributedLockService.unlock(SYNC_LOCK_KEY);
        }
    }

    /**
     * 统计新增的实体数量
     *
     * 判断依据：id 为 null 表示是新插入的（MyBatis-Plus 在 insert 前不会设置 id）
     *
     * @param entities 实体列表（Hero 或 Augment）
     * @return 新增数量
     */
    private int countInserted(List<?> entities) {
        return (int) entities.stream().filter(e -> {
            if (e instanceof Hero h) return h.getId() == null;
            if (e instanceof Augment a) return a.getId() == null;
            return false;
        }).count();
    }

    /**
     * 统计更新的实体数量
     *
     * 判断依据：id 不为 null 表示是已存在的记录被更新
     *
     * @param entities 实体列表（Hero 或 Augment）
     * @return 更新数量
     */
    private int countUpdated(List<?> entities) {
        return (int) entities.stream().filter(e -> {
            if (e instanceof Hero h) return h.getId() != null;
            if (e instanceof Augment a) return a.getId() != null;
            return false;
        }).count();
    }
}
