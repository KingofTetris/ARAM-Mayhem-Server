package com.aram.mayhem.service.impl;

import com.aram.mayhem.dto.AugmentListVO;
import com.aram.mayhem.dto.AugmentRecommendRequest;
import com.aram.mayhem.dto.AugmentRecommendResponse;
import com.aram.mayhem.dto.AugmentVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.dto.SynergyProgressResponse;
import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.mapper.AugmentMapper;
import com.aram.mayhem.mapper.HeroMapper;
import com.aram.mayhem.service.AugmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 强化符文服务实现类 —— 符文模块的"后厨"，真正做菜的地方
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类实现了 AugmentService 接口，负责强化符文模块的所有业务逻辑。
 * 强化符文是 ARAM 模式中的特殊增益效果，玩家可以在游戏中选择符文来增强英雄。
 *
 * 它负责四件核心事情：
 * 1. getAugmentList()     → 分页查询符文列表（支持品质和套装筛选）
 * 2. getAugmentDetail()   → 查询单个符文详情
 * 3. getSynergyProgress() → 计算已选符文的套装激活进度
 * 4. getRecommendations() → 基于英雄和已选符文智能推荐
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、这个类依赖了哪些"帮手"？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 依赖对象              | 作用                           | 打个比方
 * ----------------------|-------------------------------|------------------
 * AugmentMapper         | 操作 tb_augment 数据库表       | 仓库管理员（取符文数据）
 * HeroMapper            | 操作 tb_hero 数据库表          | 仓库管理员（取英雄数据）
 * StringRedisTemplate   | 操作 Redis 缓存               | 快递柜（快速存取数据）
 * Logger (log)          | 记录运行日志                   | 工作记录本
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、套装（Synergy）系统说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 套装是同类符文的组合，集齐一定数量后激活额外效果。
 * 每个符文最多可以属于3个套装（synergySet、synergySet2、synergySet3）。
 *
 * 套装名称              | 激活所需符文数   | 效果类型
 * ----------------------|-----------------|----------
 * shield（护盾）        | 1               | 防御型
 * regeneration（再生）  | 1               | 恢复型
 * shield-break（破盾）  | 1               | 破防型
 * attack-speed（攻速）  | 2               | 攻击型
 * ability-power（法强） | 2               | 法术型
 * omnivamp（全能吸血）  | 1               | 恢复型
 * armor-penetration（穿甲）| 2             | 破防型
 * critical-strike（暴击）| 2              | 攻击型
 * tenacity（韧性）      | 1               | 防御型
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、推荐算法说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 推荐评分 = 基础分(50) + 各项加成
 *
 * 评分项              | 权重/加成          | 说明
 * --------------------|-------------------|------------------
 * 胜率(winRate)       | ×30               | 胜率越高越推荐
 * 选取率(pickRate)    | ×10               | 选取率越高越热门
 * 平均排名(avgPlace)  | (4-排名)×5        | 排名越靠前越推荐
 * 套装协同            | +15               | 符文套装与英雄定位匹配时加分
 * 品质加成            | 棱彩+5/金色+3     | 高品质符文额外加分
 *
 * 排除规则：
 * - 不推荐已选择的符文（避免重复）
 * - 不推荐陷阱符文（isTrap=true，看起来强但实际弱）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、数据流转图
 * ═══════════════════════════════════════════════════════════════════
 *
 * 用户请求
 *    │
 *    ▼
 * AugmentController（接收HTTP请求）
 *    │
 *    ▼ 调用
 * AugmentServiceImpl（本类，执行业务逻辑）
 *    │
 *    ├──► getAugmentList()     → AugmentMapper.selectPage() → Entity→VO转换
 *    ├──► getAugmentDetail()   → AugmentMapper.selectById() + @Cacheable
 *    ├──► getSynergyProgress() → AugmentMapper.selectBatchIds() → 套装分组计算
 *    └──► getRecommendations() → HeroMapper + AugmentMapper → 评分算法排序
 *
 * @see com.aram.mayhem.service.AugmentService 强化符文服务接口
 * @see com.aram.mayhem.entity.Augment 符文实体类
 * @see com.aram.mayhem.mapper.AugmentMapper 符文数据访问层
 */
@Service
public class AugmentServiceImpl implements AugmentService {

    private static final Logger log = LoggerFactory.getLogger(AugmentServiceImpl.class);

    // ── 缓存相关常量 ──
    // 缓存key的前缀，完整的key格式为 "augment:list:{quality}:{synergySet}:{page}:{size}"
    private static final String CACHE_KEY_PREFIX = "augment:list:";
    // 缓存过期时间：30分钟。符文数据变化不频繁，30分钟足够
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    // ── 套装激活门槛配置 ──
    // key = 套装名称，value = 激活该套装需要的符文数量
    // 例如：shield 需要1个符文激活，attack-speed 需要2个符文激活
    private static final Map<String, Integer> SYNERGY_THRESHOLDS = new HashMap<>();
    // 所有套装名称列表，用于遍历计算进度
    private static final List<String> ALL_SYNERGIES = Arrays.asList(
            "shield", "regeneration", "shield-break", "attack-speed",
            "ability-power", "omnivamp", "armor-penetration",
            "critical-strike", "tenacity"
    );

    // 静态初始化块：在类加载时填充套装门槛配置
    // 这个块只会在类第一次被加载时执行一次
    static {
        // 防御/恢复/破防型套装：只需1个符文即可激活
        SYNERGY_THRESHOLDS.put("shield", 1);           // 护盾：1个激活
        SYNERGY_THRESHOLDS.put("regeneration", 1);     // 再生：1个激活
        SYNERGY_THRESHOLDS.put("shield-break", 1);     // 破盾：1个激活
        // 攻击/法术型套装：需要2个符文才能激活（更强力的效果需要更多投入）
        SYNERGY_THRESHOLDS.put("attack-speed", 2);     // 攻速：2个激活
        SYNERGY_THRESHOLDS.put("ability-power", 2);    // 法强：2个激活
        SYNERGY_THRESHOLDS.put("omnivamp", 1);         // 全能吸血：1个激活
        SYNERGY_THRESHOLDS.put("armor-penetration", 2);// 穿甲：2个激活
        SYNERGY_THRESHOLDS.put("critical-strike", 2);  // 暴击：2个激活
        SYNERGY_THRESHOLDS.put("tenacity", 1);         // 韧性：1个激活
    }

    private final AugmentMapper augmentMapper;
    private final HeroMapper heroMapper;
    private final StringRedisTemplate redisTemplate;

    /**
     * 构造函数 —— Spring 自动注入依赖对象
     *
     * @param augmentMapper  符文数据访问层，用于查询 tb_augment 表
     * @param heroMapper     英雄数据访问层，用于查询 tb_hero 表（推荐算法需要英雄定位信息）
     * @param redisTemplate  Redis操作模板，用于手动管理符文列表缓存
     */
    public AugmentServiceImpl(AugmentMapper augmentMapper, HeroMapper heroMapper, StringRedisTemplate redisTemplate) {
        this.augmentMapper = augmentMapper;
        this.heroMapper = heroMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取符文列表 —— 符文模块的核心查询方法
     *
     * ═══════════════════════════════════════════════════════════════
     * 功能说明
     * ═══════════════════════════════════════════════════════════════
     *
     * 分页查询强化符文列表，支持按品质和套装筛选，默认按胜率降序排列。
     *
     * ═══════════════════════════════════════════════════════════════
     * 执行流程
     * ═══════════════════════════════════════════════════════════════
     *
     * 第1步：构建缓存key（如果缓存命中则直接返回）
     * 第2步：构建查询条件（品质筛选 + 套装筛选 + 排序）
     * 第3步：执行分页查询
     * 第4步：Entity → VO 转换
     * 第5步：手动写入Redis缓存
     *
     * ═══════════════════════════════════════════════════════════════
     * 套装筛选的特殊处理
     * ═══════════════════════════════════════════════════════════════
     *
     * 每个符文最多属于3个套装（synergySet、synergySet2、synergySet3），
     * 所以筛选时需要用 OR 条件同时匹配三个字段：
     * WHERE synergy_set = 'shield' OR synergy_set2 = 'shield' OR synergy_set3 = 'shield'
     *
     * @param page        页码，从1开始
     * @param size        每页数量
     * @param quality     品质筛选（可选，null表示不筛选）
     * @param synergySet  套装筛选（可选，null表示不筛选）
     * @return PageResult<AugmentListVO> 分页符文列表
     */
    @Override
    public PageResult<AugmentListVO> getAugmentList(int page, int size, String quality, String synergySet) {
        // 构建缓存key：将所有查询参数拼接成唯一标识
        // 例如："augment:list:棱彩:shield:1:20" 表示查棱彩品质、护盾套装、第1页、每页20条
        String cacheKey = CACHE_KEY_PREFIX + quality + ":" + synergySet + ":" + page + ":" + size;

        // 创建分页参数对象
        Page<Augment> pageParam = new Page<>(page, size);
        // 创建查询条件构造器
        LambdaQueryWrapper<Augment> wrapper = new LambdaQueryWrapper<>();

        // ── 品质筛选 ──
        // 如果传入了品质参数，添加精确匹配条件
        // 例如：quality="棱彩" → WHERE quality = '棱彩'
        if (quality != null && !quality.isEmpty()) {
            wrapper.eq(Augment::getQuality, quality);
        }

        // ── 套装筛选（支持多个套装字段）──
        // 一个符文最多属于3个套装，所以需要用 OR 同时匹配三个字段
        if (synergySet != null && !synergySet.isEmpty()) {
            wrapper.and(w -> w
                    // 第1个套装字段匹配
                    .eq(Augment::getSynergySet, synergySet)
                    .or()
                    // 第2个套装字段匹配
                    .eq(Augment::getSynergySet2, synergySet)
                    .or()
                    // 第3个套装字段匹配
                    .eq(Augment::getSynergySet3, synergySet)
            );
        }

        // ── 排序：按胜率降序 ──
        // 胜率高的符文排前面，用户最关心强势符文
        wrapper.orderByDesc(Augment::getWinRate);

        // 执行分页查询
        Page<Augment> result = augmentMapper.selectPage(pageParam, wrapper);

        // ── Entity → VO 转换 ──
        // 将数据库实体转换为前端展示用的视图对象
        List<AugmentListVO> voList = result.getRecords().stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());

        // 封装分页结果返回
        return new PageResult<>(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), voList);
    }

    /**
     * 获取符文详情 —— 查看单个符文的完整信息
     *
     * ═══════════════════════════════════════════════════════════════
     * 功能说明
     * ═══════════════════════════════════════════════════════════════
     *
     * 根据符文ID获取该符文的详细信息。
     * 使用 @Cacheable 注解，查询结果会自动缓存到 Redis。
     *
     * ═══════════════════════════════════════════════════════════════
     * 缓存说明
     * ═══════════════════════════════════════════════════════════════
     *
     * - 缓存key：augmentDetail::{符文ID}
     * - unless = "#result == null"：符文不存在时不缓存null值
     *
     * @param id 符文的唯一ID（数据库主键）
     * @return AugmentVO 符文详情对象，不存在时返回null
     */
    @Override
    @Cacheable(value = "augmentDetail", key = "#id", unless = "#result == null")
    public AugmentVO getAugmentDetail(Long id) {
        // 根据ID查询数据库
        Augment augment = augmentMapper.selectById(id);
        if (augment == null) {
            // 符文不存在，返回null（不会缓存，因为unless条件）
            return null;
        }
        // Entity → VO 转换
        return convertToVO(augment);
    }

    /**
     * 计算套装激活进度 —— 根据已选符文计算各套装的完成情况
     *
     * ═══════════════════════════════════════════════════════════════
     * 功能说明
     * ═══════════════════════════════════════════════════════════════
     *
     * 玩家在游戏中选择符文后，需要知道哪些套装已经被激活、
     * 哪些套装还差几个符文就能激活。这个方法就是计算这些进度的。
     *
     * ═══════════════════════════════════════════════════════════════
     * 执行流程
     * ═══════════════════════════════════════════════════════════════
     *
     * 第1步：解析输入 → 将逗号分隔的ID字符串解析为Long列表
     * 第2步：批量查询 → 从数据库获取所有已选符文的详细信息
     * 第3步：套装分组 → 将每个符文按其所属套装归类
     * 第4步：计算进度 → 对每个套装计算当前数量/所需数量/完成百分比
     * 第5步：排序返回 → 按套装名称排序
     *
     * ═══════════════════════════════════════════════════════════════
     * 进度状态说明
     * ═══════════════════════════════════════════════════════════════
     *
     * - inactive  → 还没有选择该套装的任何符文（count=0）
     * - partial   → 已选择部分符文，但还没达到激活门槛（0 < count < threshold）
     * - completed → 已达到激活门槛，套装效果已激活（count >= threshold）
     *
     * @param augmentIds 已选符文ID列表，逗号分隔（如 "1,5,8"）
     * @return List<SynergyProgressResponse> 各套装的进度列表
     */
    @Override
    public List<SynergyProgressResponse> getSynergyProgress(String augmentIds) {
        // ── 参数校验：空输入返回空进度 ──
        // 如果没有传入任何符文ID，所有套装的状态都是 inactive
        if (augmentIds == null || augmentIds.isEmpty()) {
            return buildEmptyProgress();
        }

        // ── 解析ID列表 ──
        // 将 "1,5,8" 这样的字符串解析为 [1L, 5L, 8L] 列表
        List<Long> ids = Arrays.stream(augmentIds.split(","))  // 按逗号分割
                .map(String::trim)                               // 去除空格
                .filter(s -> !s.isEmpty())                       // 过滤空字符串
                .map(Long::parseLong)                            // 转换为Long类型
                .collect(Collectors.toList());

        // 解析后如果列表为空，也返回空进度
        if (ids.isEmpty()) {
            return buildEmptyProgress();
        }

        // ── 批量查询符文 ──
        // selectBatchIds 生成 SQL：SELECT * FROM tb_augment WHERE id IN (1, 5, 8)
        List<Augment> augments = augmentMapper.selectBatchIds(ids);

        // ── 按套装分组 ──
        // 初始化套装映射：每个套装对应一个空列表
        Map<String, List<Augment>> synergyMap = new HashMap<>();
        for (String synergy : ALL_SYNERGIES) {
            synergyMap.put(synergy, new ArrayList<>());
        }

        // ── 填充套装映射 ──
        // 遍历每个符文，将其添加到所属的套装列表中
        // 一个符文可能属于多个套装（最多3个），所以需要检查三个字段
        for (Augment augment : augments) {
            addToSynergyMap(synergyMap, augment.getSynergySet(), augment);   // 第1套装
            addToSynergyMap(synergyMap, augment.getSynergySet2(), augment);  // 第2套装
            addToSynergyMap(synergyMap, augment.getSynergySet3(), augment);  // 第3套装
        }

        // ── 构建进度响应并排序 ──
        // 对每个套装计算进度，然后按套装名称排序
        return synergyMap.entrySet().stream()
                .map(entry -> buildSynergyProgress(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(SynergyProgressResponse::getSynergyName))
                .collect(Collectors.toList());
    }

    /**
     * 智能推荐符文 —— 根据英雄和已选符文推荐下一个符文
     *
     * ═══════════════════════════════════════════════════════════════
     * 功能说明
     * ═══════════════════════════════════════════════════════════════
     *
     * 这是符文模块的"智能大脑"，根据英雄定位和已选符文，
     * 推荐最合适的下一个符文。
     *
     * ═══════════════════════════════════════════════════════════════
     * 执行流程
     * ═══════════════════════════════════════════════════════════════
     *
     * 第1步：查询英雄信息 → 获取英雄的角色定位（如 Tank、Mage）
     * 第2步：构建排除集合 → 已选符文ID + 陷阱符文不参与推荐
     * 第3步：查询候选符文 → 排除已选和陷阱，按胜率排序取前20
     * 第4步：评分计算 → 对每个候选符文计算推荐评分
     * 第5步：排序返回 → 按评分从高到低排列
     *
     * @param request 推荐请求体，包含 heroId 和 selectedAugmentIds
     * @return List<AugmentRecommendResponse> 推荐符文列表（按评分降序）
     */
    @Override
    public List<AugmentRecommendResponse> getRecommendations(AugmentRecommendRequest request) {
        log.info("Getting augment recommendations: heroId={}, selectedAugments={}", request.getHeroId(), request.getSelectedAugmentIds());

        // ── 第1步：查询英雄信息 ──
        // 需要英雄的角色定位来判断套装协同
        Hero hero = heroMapper.selectById(request.getHeroId());
        if (hero == null) {
            // 英雄不存在，返回空列表（不抛异常，推荐功能不影响核心流程）
            log.warn("Hero not found for recommendations: heroId={}", request.getHeroId());
            return new ArrayList<>();
        }

        // ── 第2步：构建排除集合 ──
        // 已选择的符文不再推荐（避免重复选择）
        Set<Long> excludeIds = new HashSet<>(request.getSelectedAugmentIds());

        // ── 第3步：查询候选符文 ──
        LambdaQueryWrapper<Augment> wrapper = new LambdaQueryWrapper<>();
        // 排除已选符文（如果排除列表不为空）
        wrapper.notIn(!excludeIds.isEmpty(), Augment::getId, excludeIds);
        // 排除陷阱符文（isTrap=true 的符文看起来强但实际弱）
        wrapper.eq(Augment::getIsTrap, false);
        // 按胜率降序，取前20个作为候选
        wrapper.orderByDesc(Augment::getWinRate);
        Page<Augment> page = new Page<>(1, 20);
        Page<Augment> result = augmentMapper.selectPage(page, wrapper);

        // 获取英雄的角色定位（如 "Tank"、"Mage"）
        String heroRole = hero.getRole();

        // ── 第4步+第5步：评分计算并排序 ──
        // 对每个候选符文计算推荐评分，然后按评分降序排列
        return result.getRecords().stream()
                .map(augment -> convertToRecommendResponse(augment, heroRole, request.getSelectedAugmentIds()))
                .sorted(Comparator.comparingDouble(AugmentRecommendResponse::getScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 将符文添加到套装映射中（私有辅助方法）
     *
     * 检查符文的某个套装字段是否匹配已知套装名称，
     * 如果匹配则将符文添加到对应套装的列表中。
     *
     * @param synergyMap 套装映射（key=套装名，value=符文列表）
     * @param synergy    符文的套装属性值（可能为null或空字符串）
     * @param augment    要添加的符文对象
     */
    private void addToSynergyMap(Map<String, List<Augment>> synergyMap, String synergy, Augment augment) {
        // 只有当套装名不为空且在已知套装列表中时才添加
        if (synergy != null && !synergy.isEmpty() && synergyMap.containsKey(synergy)) {
            synergyMap.get(synergy).add(augment);
        }
    }

    /**
     * 构建空进度列表（私有辅助方法）
     *
     * 当没有传入任何符文ID时，所有套装的状态都是 inactive（未激活）。
     *
     * @return List<SynergyProgressResponse> 所有套装的初始进度（全部inactive）
     */
    private List<SynergyProgressResponse> buildEmptyProgress() {
        return ALL_SYNERGIES.stream()
                .map(synergy -> {
                    SynergyProgressResponse resp = new SynergyProgressResponse();
                    resp.setSynergyName(synergy);                                    // 套装名称
                    resp.setCurrentCount(0);                                          // 当前数量：0
                    resp.setTotalCount(SYNERGY_THRESHOLDS.getOrDefault(synergy, 1));  // 激活所需数量
                    resp.setProgress(0.0);                                            // 进度：0%
                    resp.setStatus("inactive");                                       // 状态：未激活
                    return resp;
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建单个套装的进度信息（私有辅助方法）
     *
     * ═══════════════════════════════════════════════════════════════
     * 计算逻辑
     * ═══════════════════════════════════════════════════════════════
     *
     * 1. 去重计数：同一个符文可能被添加多次（因为它属于多个套装字段），
     *    使用 .distinct() 确保不重复计数
     * 2. 进度计算：progress = min(1.0, count / total)
     *    使用 min 防止进度超过100%
     * 3. 状态判定：
     *    - count == 0 → inactive
     *    - count >= total → completed
     *    - 其他 → partial
     * 4. 平均胜率：计算该套装下所有已选符文的平均胜率
     *
     * @param synergyName 套装名称
     * @param augments    该套装下的已选符文列表
     * @return SynergyProgressResponse 套装进度信息
     */
    private SynergyProgressResponse buildSynergyProgress(String synergyName, List<Augment> augments) {
        // 获取激活该套装所需的符文数量
        int total = SYNERGY_THRESHOLDS.getOrDefault(synergyName, 1);
        // 去重计数：同一个符文可能因为属于多个套装字段而被重复添加
        int count = (int) augments.stream().distinct().count();
        // 计算完成进度（0.0~1.0），使用min防止超过1.0
        double progress = Math.min(1.0, (double) count / total);

        // 判定进度状态
        String status;
        if (count == 0) {
            status = "inactive";    // 未选择任何该套装的符文
        } else if (count >= total) {
            status = "completed";   // 已达到激活门槛
        } else {
            status = "partial";     // 部分完成
        }

        // 计算该套装下已选符文的平均胜率
        // 过滤掉null值，求和后除以数量，保留4位小数
        BigDecimal avgWinRate = augments.stream()
                .map(Augment::getWinRate)
                .filter(wr -> wr != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1, augments.size())), 4, BigDecimal.ROUND_HALF_UP);

        // 构建响应对象
        SynergyProgressResponse resp = new SynergyProgressResponse();
        resp.setSynergyName(synergyName);
        resp.setCurrentCount(count);
        resp.setTotalCount(total);
        resp.setProgress(progress);
        resp.setStatus(status);
        resp.setAvgWinRate(avgWinRate);
        return resp;
    }

    /**
     * Augment Entity → AugmentRecommendResponse 转换（私有方法）
     *
     * 将符文实体转换为推荐响应对象，同时计算推荐评分和推荐理由。
     *
     * @param augment     符文实体
     * @param heroRole    英雄角色定位（用于判断套装协同）
     * @param selectedIds 已选符文ID列表（当前未使用，预留扩展）
     * @return AugmentRecommendResponse 推荐响应对象
     */
    private AugmentRecommendResponse convertToRecommendResponse(Augment augment, String heroRole, List<Long> selectedIds) {
        AugmentRecommendResponse resp = new AugmentRecommendResponse();
        // 复制基本信息
        resp.setId(augment.getId());
        resp.setNameZh(augment.getNameZh());
        resp.setNameEn(augment.getNameEn());
        resp.setQuality(augment.getQuality());
        resp.setSynergySet(augment.getSynergySet());
        resp.setIconUrl(augment.getIconUrl());
        resp.setWinRate(augment.getWinRate());
        resp.setPickRate(augment.getPickRate());
        resp.setAvgPlacement(augment.getAvgPlacement());
        resp.setTier(augment.getTier());
        resp.setIsTrap(augment.getIsTrap());

        // 计算推荐评分（0~100分）
        double score = calculateScore(augment, heroRole, selectedIds);
        resp.setScore(score);
        // 生成推荐理由（如"高胜率 契合当前英雄定位 顶级品质"）
        resp.setRecommendationReason(generateReason(augment, heroRole));

        return resp;
    }

    /**
     * 计算推荐评分（私有方法）—— 推荐算法的核心
     *
     * ═══════════════════════════════════════════════════════════════
     * 评分算法详解
     * ═══════════════════════════════════════════════════════════════
     *
     * 基础分 = 50（所有符文的起步分）
     *
     * 加分项：
     * ┌──────────────────┬──────────────────┬──────────────────────────────┐
     * │ 评分项           │ 计算方式          │ 说明                          │
     * ├──────────────────┼──────────────────┼──────────────────────────────┤
     * │ 胜率加成         │ winRate × 30     │ 胜率50%加15分，55%加16.5分    │
     * │ 选取率加成       │ pickRate × 10    │ 选取率10%加1分                │
     * │ 平均排名加成     │ (4-avgPlace)×5   │ 排名第1加15分，第4加0分       │
     * │ 套装协同加成     │ +15              │ 符文套装与英雄定位匹配时      │
     * │ 品质加成         │ 棱彩+5/金色+3    │ 高品质符文额外加分            │
     * └──────────────────┴──────────────────┴──────────────────────────────┘
     *
     * 最终分数限制在 0~100 之间
     *
     * @param augment     符文实体
     * @param heroRole    英雄角色定位
     * @param selectedIds 已选符文ID列表
     * @return double 推荐评分（0~100）
     */
    private double calculateScore(Augment augment, String heroRole, List<Long> selectedIds) {
        // 基础分：所有符文的起步分
        double score = 50.0;

        // ── 胜率加成 ──
        // winRate 范围通常在 40~60 之间，乘以30后贡献约 12~18 分
        if (augment.getWinRate() != null) {
            score += augment.getWinRate().doubleValue() * 30;
        }

        // ── 选取率加成 ──
        // pickRate 范围通常在 0~30 之间，乘以10后贡献约 0~3 分
        if (augment.getPickRate() != null) {
            score += augment.getPickRate().doubleValue() * 10;
        }

        // ── 平均排名加成 ──
        // avgPlacement 范围在 1~8 之间，排名越靠前（值越小）加分越多
        // (4-1)×5=15分（排名第1），(4-4)×5=0分（排名第4）
        if (augment.getAvgPlacement() != null && augment.getAvgPlacement().compareTo(BigDecimal.ZERO) > 0) {
            score += (4.0 - augment.getAvgPlacement().doubleValue()) * 5;
        }

        // ── 套装协同加成 ──
        // 如果符文的套装与英雄定位匹配，额外加15分
        // 例如：坦克英雄 + 护盾套装 = 匹配，加分
        String synergy = augment.getSynergySet();
        if (synergy != null && isRoleSynergyMatch(synergy, heroRole)) {
            score += 15;
        }

        // ── 品质加成 ──
        // 棱彩（最稀有）+5分，金色（中等）+3分
        if ("PRISMATIC".equals(augment.getQuality())) {
            score += 5;
        } else if ("LEGENDARY".equals(augment.getQuality())) {
            score += 3;
        }

        // 将分数限制在 0~100 之间
        return Math.max(0, Math.min(100, score));
    }

    /**
     * 判断符文套装是否与英雄定位匹配（私有方法）
     *
     * ═══════════════════════════════════════════════════════════════
     * 匹配规则
     * ═══════════════════════════════════════════════════════════════
     *
     * 英雄定位          | 匹配的套装
     * ------------------|--------------------------
     * Tank/Support      | shield（护盾）
     * Support/Healer    | regeneration（再生）
     * Marksman/Assassin | attack-speed（攻速）、critical-strike（暴击）
     * Mage/AP           | ability-power（法强）
     *
     * @param synergy 符文套装名称
     * @param role    英雄角色定位
     * @return true=匹配，false=不匹配
     */
    private boolean isRoleSynergyMatch(String synergy, String role) {
        if (role == null) return false;
        role = role.toLowerCase();  // 统一转小写比较

        // 坦克/辅助 + 护盾套装 → 匹配
        if (synergy.contains("shield") && (role.contains("tank") || role.contains("support"))) {
            return true;
        }
        // 辅助/治疗 + 再生套装 → 匹配
        if (synergy.contains("regeneration") && (role.contains("support") || role.contains("healer"))) {
            return true;
        }
        // 射手/刺客 + 攻速套装 → 匹配
        if (synergy.contains("attack-speed") && (role.contains("marksman") || role.contains("assassin"))) {
            return true;
        }
        // 法师 + 法强套装 → 匹配
        if (synergy.contains("ability-power") && (role.contains("mage") || role.contains("ap"))) {
            return true;
        }
        // 射手/刺客 + 暴击套装 → 匹配
        if (synergy.contains("critical-strike") && (role.contains("marksman") || role.contains("assassin"))) {
            return true;
        }

        return false;
    }

    /**
     * 生成推荐理由（私有方法）
     *
     * 根据符文的属性和英雄定位，生成人类可读的推荐理由。
     * 例如："高胜率 契合当前英雄定位 顶级品质"
     *
     * @param augment  符文实体
     * @param heroRole 英雄角色定位
     * @return String 推荐理由文本
     */
    private String generateReason(Augment augment, String heroRole) {
        StringBuilder reason = new StringBuilder();

        // 胜率超过50% → 标注"高胜率"
        if (augment.getWinRate() != null && augment.getWinRate().compareTo(new BigDecimal("50")) > 0) {
            reason.append("高胜率 ");
        }

        // 套装与英雄定位匹配 → 标注"契合当前英雄定位"
        if (isRoleSynergyMatch(augment.getSynergySet(), heroRole)) {
            reason.append("契合当前英雄定位 ");
        }

        // 品质标注
        if ("PRISMATIC".equals(augment.getQuality())) {
            reason.append("顶级品质");
        } else if ("LEGENDARY".equals(augment.getQuality())) {
            reason.append("高品质");
        }

        // 如果以上条件都不满足，标注"综合表现优秀"
        if (reason.length() == 0) {
            reason.append("综合表现优秀");
        }

        return reason.toString().trim();
    }

    /**
     * Augment Entity → AugmentListVO 转换（私有方法）
     *
     * 将符文实体转换为列表页展示用的视图对象。
     * 只保留列表页需要的字段，减少数据传输量。
     *
     * @param augment 符文实体
     * @return AugmentListVO 列表页视图对象
     */
    private AugmentListVO convertToListVO(Augment augment) {
        AugmentListVO vo = new AugmentListVO();
        vo.setId(augment.getId());
        vo.setNameZh(augment.getNameZh());
        vo.setNameEn(augment.getNameEn());
        vo.setQuality(augment.getQuality());
        vo.setSynergySet(augment.getSynergySet());
        vo.setIconUrl(augment.getIconUrl());
        vo.setWinRate(augment.getWinRate());
        vo.setPickRate(augment.getPickRate());
        vo.setAvgPlacement(augment.getAvgPlacement());
        vo.setTier(augment.getTier());
        return vo;
    }

    /**
     * Augment Entity → AugmentVO 转换（私有方法）
     *
     * 将符文实体转换为详情页展示用的视图对象。
     * 使用 BeanUtils.copyProperties 批量复制所有同名字段。
     *
     * 为什么这里可以用 BeanUtils 而 Hero 不行？
     * 因为 Augment 和 AugmentVO 的字段名完全一致，
     * 而 Hero 和 HeroListVO 的字段不完全一致（需要额外处理关联数据）。
     *
     * @param augment 符文实体
     * @return AugmentVO 详情页视图对象
     */
    private AugmentVO convertToVO(Augment augment) {
        AugmentVO vo = new AugmentVO();
        // BeanUtils.copyProperties 会自动复制所有同名字段
        // 前提：源对象和目标对象的字段名和类型必须一致
        BeanUtils.copyProperties(augment, vo);
        return vo;
    }
}
