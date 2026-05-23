package com.aram.mayhem.service.impl;

import com.aram.mayhem.dto.AugmentListVO;
import com.aram.mayhem.dto.HeroDetailVO;
import com.aram.mayhem.dto.HeroListVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.mapper.AugmentMapper;
import com.aram.mayhem.mapper.HeroMapper;
import com.aram.mayhem.service.CacheWarmupService;
import com.aram.mayhem.service.HeroService;
import com.aram.mayhem.service.AugmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 缓存预热服务实现类 —— 系统启动时的"提前烧水"服务
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类实现了 CacheWarmupService 接口，负责在系统启动时或定时任务中，
 * 提前把热点数据加载到 Redis 缓存中。
 *
 * 为什么要"预热"缓存？
 * - 如果不做预热，第一个访问的用户需要等数据库查询，响应会很慢
 * - 预热后，用户第一次访问就能直接从缓存获取数据，体验更好
 * - 类比：饭店开门前先把水烧开，客人来了直接倒茶，不用等烧水
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、这个类依赖了哪些"帮手"？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 依赖对象              | 作用                           | 打个比方
 * ----------------------|-------------------------------|------------------
 * HeroMapper            | 操作 tb_hero 数据库表          | 仓库管理员（取英雄数据）
 * AugmentMapper         | 操作 tb_augment 数据库表       | 仓库管理员（取符文数据）
 * HeroService           | 英雄服务（触发缓存写入）       | 厨师（做菜并上菜）
 * AugmentService        | 符文服务（触发缓存写入）       | 厨师（做菜并上菜）
 * RedisTemplate         | 操作 Redis 缓存               | 保温柜（存放预热好的数据）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、缓存策略说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 缓存类型              | 缓存Key前缀          | TTL（过期时间）    | 说明
 * ──────────────────────────────────────────────────────────────────────
 * 英雄详情缓存          | heroDetail::         | 6小时              | 热门英雄的详细数据
 * 符文列表缓存          | augment:list:        | 30分钟             | 符文分页列表数据
 * 英雄列表缓存          | hero:list:all        | 6小时              | 全部英雄列表数据
 *
 * 为什么 TTL 不同？
 * - 英雄数据变化频率低（赛季更新才变），TTL 可以长一些（6小时）
 * - 符文数据可能更频繁调整，TTL 短一些（30分钟）
 * - 英雄列表是全量数据，变化更少，TTL 与英雄详情一致
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、Lombok 注解说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * @Slf4j —— 自动生成 Logger 对象，等价于：
 *   private static final Logger log = LoggerFactory.getLogger(CacheWarmupServiceImpl.class);
 *   好处：减少样板代码，不需要手动声明日志对象
 *
 * @RequiredArgsConstructor —— 自动生成包含 final 字段的构造函数
 *   等价于手写构造函数，Spring 通过构造函数注入依赖
 *   好处：不需要写 @Autowired 注解，代码更简洁
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、方法总览
 * ═══════════════════════════════════════════════════════════════════
 *
 * 方法名                  | 功能                              | 是否事务
 * ------------------------|----------------------------------|----------
 * warmupHeroCache()       | 预热热门英雄详情缓存              | 否
 * warmupAugmentCache()    | 预热符文列表缓存                  | 否
 * warmupHeroListCache()   | 预热英雄列表缓存                  | 否
 * cleanupStaleCache()     | 清理过期的缓存数据                | 否
 * convertToListVO()       | 实体→列表VO转换（私有）           | 否
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheWarmupServiceImpl implements CacheWarmupService {

    /**
     * 英雄详情缓存的Key前缀
     *
     * 完整的缓存Key格式：heroDetail::{英雄ID}
     * 例如：heroDetail::1、heroDetail::2
     *
     * 这个前缀需要与 HeroServiceImpl 中 @Cacheable 的 key 保持一致，
     * 否则预热写入的缓存无法被 HeroServiceImpl 的查询方法命中。
     */
    private static final String HERO_DETAIL_CACHE_PREFIX = "heroDetail";

    /**
     * 符文列表缓存的Key前缀
     *
     * 完整的缓存Key格式：augment:list:{quality}:{synergySet}:{page}:{size}
     * 例如：augment:list:null:null:1:200
     */
    private static final String AUGMENT_LIST_CACHE_PREFIX = "augment:list:";

    /**
     * 英雄列表缓存的Key
     *
     * 固定Key：hero:list:all
     * 因为英雄列表是全量数据，不需要按条件区分
     */
    private static final String HERO_LIST_CACHE_KEY = "hero:list:all";

    /**
     * 英雄详情缓存的TTL（Time To Live，存活时间）
     *
     * Duration.ofHours(6) = 6小时
     * 6小时后缓存自动过期，下次访问会重新从数据库加载
     */
    private static final Duration HERO_CACHE_TTL = Duration.ofHours(6);

    /**
     * 符文列表缓存的TTL
     *
     * Duration.ofMinutes(30) = 30分钟
     * 符文数据可能更频繁调整，TTL比英雄短
     */
    private static final Duration AUGMENT_CACHE_TTL = Duration.ofMinutes(30);

    /**
     * 英雄列表缓存的TTL
     *
     * 与英雄详情一致，6小时
     */
    private static final Duration HERO_LIST_CACHE_TTL = Duration.ofHours(6);

    /**
     * 符文预热时每页查询的数量
     *
     * 设置为200是为了减少分页查询次数，一次多查一些
     */
    private static final int AUGMENT_PAGE_SIZE = 200;

    /**
     * 英雄数据访问对象 —— 用于查询热门英雄列表
     */
    private final HeroMapper heroMapper;

    /**
     * 符文数据访问对象 —— 用于查询符文列表
     */
    private final AugmentMapper augmentMapper;

    /**
     * 英雄服务 —— 用于触发英雄详情的缓存写入
     *
     * 为什么不直接查数据库然后写缓存？
     * 因为 HeroService.getHeroDetail() 方法上有 @Cacheable 注解，
     * 调用它时会自动将结果写入缓存。这样保证了缓存Key和格式的一致性。
     */
    private final HeroService heroService;

    /**
     * 符文服务 —— 用于触发符文列表的缓存写入
     *
     * 与 HeroService 类似，调用 AugmentService 的方法会自动触发缓存写入
     */
    private final AugmentService augmentService;

    /**
     * Redis 操作模板 —— 用于直接操作缓存
     *
     * RedisTemplate<String, Object> 可以存储任意 Java 对象（需要序列化配置）
     * 与 StringRedisTemplate 不同，它支持存储复杂对象
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 预热热门英雄详情缓存 —— 系统启动时提前加载热门英雄数据
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 在系统启动时，查询胜率最高的 topN 个英雄，提前将它们的详情加载到缓存中。
     * 这样用户第一次访问这些热门英雄时，就能直接从缓存获取数据。
     *
     * @param topN 预热的英雄数量，例如 topN=20 表示预热胜率前20的英雄
     * @return List<Long> 成功预热的英雄ID列表
     *
     * ══════════════════════════════════════════════════════════════
     * 执行流程
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 查询胜率最高的 topN 个英雄
     * 2. 逐个调用 heroService.getHeroDetail() 触发缓存写入
     * 3. 记录成功和失败的数量
     * 4. 返回成功预热的英雄ID列表
     *
     * ══════════════════════════════════════════════════════════════
     * 为什么用 heroService.getHeroDetail() 而不是直接写缓存？
     * ══════════════════════════════════════════════════════════════
     *
     * heroService.getHeroDetail() 上有 @Cacheable 注解，
     * 调用它时会自动将结果写入 Redis，缓存Key和格式由注解控制。
     * 如果我们手动写缓存，Key格式可能不一致，导致查询时无法命中。
     */
    @Override
    public List<Long> warmupHeroCache(int topN) {
        log.info("[WARMUP] hero cache warmup started | topN={}", topN);
        long startTime = System.currentTimeMillis();

        // ─── 查询胜率最高的 topN 个英雄 ───
        // orderByDesc(Hero::getWinRate) → 按胜率降序排列
        // .last("LIMIT " + topN) → 只取前 topN 条
        LambdaQueryWrapper<Hero> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Hero::getWinRate);
        wrapper.last("LIMIT " + topN);
        List<Hero> topHeroes = heroMapper.selectList(wrapper);

        // ─── 逐个预热英雄详情缓存 ───
        List<Long> warmedUpIds = new ArrayList<>();
        int success = 0;
        int failed = 0;

        for (Hero hero : topHeroes) {
            try {
                // 调用 heroService.getHeroDetail() 会触发 @Cacheable 注解
                // 如果缓存中没有数据，会查询数据库并将结果写入缓存
                HeroDetailVO detail = heroService.getHeroDetail(hero.getId());
                if (detail != null) {
                    warmedUpIds.add(hero.getId());
                    success++;
                }
            } catch (Exception e) {
                // 单个英雄预热失败不影响其他英雄
                // 记录警告日志，继续处理下一个
                failed++;
                log.warn("[WARMUP] failed to warmup hero cache | id={} | name={} | error={}",
                        hero.getId(), hero.getNameEn(), e.getMessage());
            }
        }

        // ─── 记录预热结果 ───
        long durationMs = System.currentTimeMillis() - startTime;
        log.info("[WARMUP] hero cache warmup completed | total={} | success={} | failed={} | duration={}ms",
                topHeroes.size(), success, failed, durationMs);
        return warmedUpIds;
    }

    /**
     * 预热符文列表缓存 —— 系统启动时提前加载符文数据
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 在系统启动时，查询所有符文数据，按分页方式提前加载到缓存中。
     * 因为符文数量可能很多（几百个），所以采用分页方式预热。
     *
     * @return int 成功预热的符文总数
     *
     * ══════════════════════════════════════════════════════════════
     * 分页预热策略
     * ══════════════════════════════════════════════════════════════
     *
     * 符文数据量可能很大，不能一次全部加载（会占用大量内存）。
     * 所以采用分页方式，每页200条，逐页预热：
     * - 第1页：1~200
     * - 第2页：201~400
     * - ...直到某页数据不足200条，说明已经预热完毕
     *
     * ══════════════════════════════════════════════════════════════
     * 为什么不调用 augmentService 来触发缓存？
     * ══════════════════════════════════════════════════════════════
     *
     * 与英雄预热不同，这里直接将结果写入 Redis，
     * 因为符文列表的缓存Key格式需要手动构造。
     * 缓存Key格式：augment:list:{quality}:{synergySet}:{page}:{size}
     */
    @Override
    public int warmupAugmentCache() {
        log.info("[WARMUP] augment cache warmup started");
        long startTime = System.currentTimeMillis();

        int totalWarmed = 0;
        int page = 1;

        // ─── 分页循环预热 ───
        // while(true) 循环，直到某页数据为空或不足一页时退出
        while (true) {
            try {
                // 查询当前页的符文数据
                // quality=null, synergySet=null 表示查询所有品质和套装的符文
                PageResult<AugmentListVO> result = augmentService.getAugmentList(page, AUGMENT_PAGE_SIZE, null, null);

                // 如果当前页没有数据，说明所有页都已预热完毕
                if (result.getRecords() == null || result.getRecords().isEmpty()) {
                    break;
                }

                // 累加预热的符文数量
                totalWarmed += result.getRecords().size();

                // ─── 手动写入缓存 ───
                // 构造缓存Key：augment:list:null:null:{page}:{size}
                String cacheKey = AUGMENT_LIST_CACHE_PREFIX + "null:null:" + page + ":" + AUGMENT_PAGE_SIZE;
                redisTemplate.opsForValue().set(cacheKey, result, AUGMENT_CACHE_TTL);

                // 如果当前页数据不足一页，说明已经是最后一页了
                if (result.getRecords().size() < AUGMENT_PAGE_SIZE) {
                    break;
                }

                // 继续预热下一页
                page++;
            } catch (Exception e) {
                // 预热失败则停止，避免无限循环
                log.warn("[WARMUP] failed to warmup augment cache at page={} | error={}", page, e.getMessage());
                break;
            }
        }

        long durationMs = System.currentTimeMillis() - startTime;
        log.info("[WARMUP] augment cache warmup completed | totalWarmed={} | duration={}ms", totalWarmed, durationMs);
        return totalWarmed;
    }

    /**
     * 预热英雄列表缓存 —— 系统启动时提前加载全部英雄列表
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 在系统启动时，查询所有英雄数据，转换为列表VO后存入缓存。
     * 与 warmupHeroCache() 不同，这个方法预热的是"英雄列表"而不是"英雄详情"。
     *
     * @return int 成功预热的英雄总数
     *
     * ══════════════════════════════════════════════════════════════
     * 英雄列表 vs 英雄详情的区别
     * ══════════════════════════════════════════════════════════════
     *
     * - 英雄列表（HeroListVO）：包含基本信息（名称、胜率、头像等），用于列表展示
     * - 英雄详情（HeroDetailVO）：包含完整信息（推荐符文、装备等），用于详情页面
     *
     * 列表数据量小，可以一次性加载全部；详情数据量大，只预热热门的。
     */
    @Override
    public int warmupHeroListCache() {
        log.info("[WARMUP] hero list cache warmup started");
        long startTime = System.currentTimeMillis();

        int totalWarmed = 0;

        // ─── 查询所有英雄 ───
        // orderByDesc(Hero::getWinRate) → 按胜率降序排列
        LambdaQueryWrapper<Hero> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Hero::getWinRate);
        List<Hero> allHeroes = heroMapper.selectList(wrapper);

        // ─── 转换为列表VO ───
        List<HeroListVO> heroListVOs = allHeroes.stream()
                .map(this::convertToListVO)
                .toList();

        // ─── 写入缓存 ───
        // Key: hero:list:all
        // Value: 全部英雄的列表VO
        // TTL: 6小时
        redisTemplate.opsForValue().set(HERO_LIST_CACHE_KEY, heroListVOs, HERO_LIST_CACHE_TTL);
        totalWarmed = heroListVOs.size();

        long durationMs = System.currentTimeMillis() - startTime;
        log.info("[WARMUP] hero list cache warmup completed | totalWarmed={} | duration={}ms", totalWarmed, durationMs);
        return totalWarmed;
    }

    /**
     * 清理过期缓存 —— 定期清理数据库中已不存在的英雄的缓存
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 当数据库中的英雄被删除后，Redis 中可能还残留着该英雄的缓存数据。
     * 这个方法会检查所有英雄详情缓存，删除那些数据库中已不存在的缓存。
     *
     * @return int 清理的缓存数量
     *
     * ══════════════════════════════════════════════════════════════
     * 执行流程
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 从 Redis 中获取所有英雄详情缓存的Key
     * 2. 从数据库中获取所有英雄的ID
     * 3. 对比：如果缓存中的英雄ID在数据库中不存在，删除该缓存
     * 4. 检查符文缓存是否有过期Key（已由TTL自动处理）
     *
     * ══════════════════════════════════════════════════════════════
     * ⚠️ 注意：keys() 命令的性能问题
     * ══════════════════════════════════════════════════════════════
     *
     * redisTemplate.keys() 会扫描整个 Redis，在数据量大时性能较差。
     * 生产环境建议使用 SCAN 命令替代，或者维护一个缓存Key的索引集合。
     */
    @Override
    public int cleanupStaleCache() {
        log.info("[WARMUP] stale cache cleanup started");
        long startTime = System.currentTimeMillis();

        int cleaned = 0;

        // ─── 清理英雄详情的过期缓存 ───
        // 获取所有以 "heroDetail::" 开头的缓存Key
        Set<String> heroDetailKeys = redisTemplate.keys(HERO_DETAIL_CACHE_PREFIX + "::*");
        if (heroDetailKeys != null && !heroDetailKeys.isEmpty()) {
            // 查询数据库中所有英雄的ID，构建有效ID集合
            List<Hero> allHeroes = heroMapper.selectList(null);
            Set<Long> validHeroIds = allHeroes.stream()
                    .map(Hero::getId)
                    .collect(java.util.stream.Collectors.toSet());

            // 逐个检查缓存Key对应的英雄ID是否在数据库中存在
            for (String key : heroDetailKeys) {
                // 从Key中提取英雄ID
                // Key格式：heroDetail::123 → 提取出 "123"
                String idPart = key.substring(key.lastIndexOf("::") + 2);
                try {
                    Long cachedId = Long.parseLong(idPart);
                    // 如果数据库中不存在该英雄ID，删除缓存
                    if (!validHeroIds.contains(cachedId)) {
                        redisTemplate.delete(key);
                        cleaned++;
                        log.debug("[WARMUP] deleted stale hero cache | key={}", key);
                    }
                } catch (NumberFormatException ignored) {
                    // Key格式异常，跳过
                }
            }
        }

        // ─── 检查符文缓存的过期情况 ───
        // 符文缓存主要依赖TTL自动过期，这里只做检查和日志记录
        Set<String> augmentListKeys = redisTemplate.keys(AUGMENT_LIST_CACHE_PREFIX + "*");
        if (augmentListKeys != null && !augmentListKeys.isEmpty()) {
            // 统计已过期的Key数量（TTL < 0 表示已过期或不存在）
            long expiredCount = augmentListKeys.stream()
                    .filter(key -> redisTemplate.getExpire(key) != null && redisTemplate.getExpire(key) < 0)
                    .count();
            if (expiredCount > 0) {
                log.info("[WARMUP] found {} expired augment cache keys (already handled by TTL)", expiredCount);
            }
        }

        long durationMs = System.currentTimeMillis() - startTime;
        log.info("[WARMUP] stale cache cleanup completed | cleaned={} | duration={}ms", cleaned, durationMs);
        return cleaned;
    }

    /**
     * 实体转列表VO —— 将 Hero 实体转换为 HeroListVO
     *
     * 这是一个私有辅助方法，用于在预热英雄列表缓存时将实体转换为VO。
     * 与 HeroServiceImpl 中的转换逻辑类似，但这里是独立实现的，
     * 因为预热服务不依赖 HeroServiceImpl 的列表查询方法。
     *
     * @param hero 英雄实体对象
     * @return HeroListVO 列表视图对象
     */
    private HeroListVO convertToListVO(Hero hero) {
        HeroListVO vo = new HeroListVO();
        vo.setId(hero.getId());
        vo.setNameEn(hero.getNameEn());
        vo.setNameZh(hero.getNameZh());
        vo.setTitle(hero.getTitle());
        vo.setRole(hero.getRole());
        vo.setTier(hero.getTier());
        vo.setWinRate(hero.getWinRate());
        vo.setPickRate(hero.getPickRate());
        vo.setImageUrl(hero.getImageUrl());
        vo.setIsVersionTrap(hero.getIsVersionTrap());
        return vo;
    }
}
