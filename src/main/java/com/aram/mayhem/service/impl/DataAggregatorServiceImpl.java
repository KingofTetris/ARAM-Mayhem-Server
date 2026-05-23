package com.aram.mayhem.service.impl;

import com.aram.mayhem.client.AramDataCollector;
import com.aram.mayhem.client.RiotDataDragonClient;
import com.aram.mayhem.dto.AramAugmentStatsDTO;
import com.aram.mayhem.dto.AramHeroStatsDTO;
import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.mapper.AugmentMapper;
import com.aram.mayhem.mapper.HeroMapper;
import com.aram.mayhem.service.DataAggregatorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据聚合服务实现类 —— 多数据源合并的"数据加工厂"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类实现了 DataAggregatorService 接口，负责从多个数据源采集数据，
 * 合并、清洗后生成最终的英雄和符文实体。它是整个数据管线的核心环节。
 *
 * 数据管线流程：
 * ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
 * │ 数据源1       │    │ 数据源2       │    │              │    │              │
 * │ Riot DataDragon│──→│ 本类（聚合）  │──→│ 数据库存储    │──→│ 缓存预热     │
 * │ (基础信息)    │    │ (合并+清洗)  │    │              │    │              │
 * │ 数据源3       │    │              │    │              │    │              │
 * │ U.GG (统计数据)│──→│              │    │              │    │              │
 * └──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、数据源说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 数据源                    | 提供的数据              | 调用方式
 * ──────────────────────────────────────────────────────────────────────
 * Riot DataDragon           | 英雄名称、头像、技能等  | riotDataDragonClient
 * U.GG / ARAM数据采集器     | 胜率、选取率、段位等    | aramDataCollector
 *
 * 为什么要用两个数据源？
 * - Riot DataDragon 提供官方的英雄基础信息（名称、技能描述、头像URL等）
 * - U.GG 提供 ARAM 模式的统计数据（胜率、选取率、段位评级等）
 * - 两个数据源互补，合并后才能得到完整的英雄信息
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据清洗策略
 * ═══════════════════════════════════════════════════════════════════
 *
 * 从外部数据源获取的数据可能存在异常值，需要清洗：
 *
 * 清洗规则              | 说明
 * ──────────────────────────────────────────────────────────────────────
 * clamp（范围限制）     | 胜率限制在0~100，选取率限制在0~100
 * sanitizeTier          | 段位只允许 S+/S/A/B/C，其他替换为默认值C
 * sanitizeQuality       | 品质只允许 银色/金色/棱彩，英文自动转中文
 * fillDefault           | 无统计数据时填充默认值（胜率50%，段位C）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、这个类依赖了哪些"帮手"？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 依赖对象                | 作用                           | 打个比方
 * ------------------------|-------------------------------|------------------
 * RiotDataDragonClient    | 从Riot获取英雄基础信息          | 原料供应商A
 * AramDataCollector       | 从U.GG获取ARAM统计数据          | 原料供应商B
 * HeroMapper              | 操作 tb_hero 数据库表           | 仓库管理员
 * AugmentMapper           | 操作 tb_augment 数据库表        | 仓库管理员
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、方法总览
 * ═══════════════════════════════════════════════════════════════════
 *
 * 方法名                      | 功能                              | 是否事务
 * ----------------------------|----------------------------------|----------
 * aggregateHeroData()         | 聚合英雄数据（不保存）            | 否
 * aggregateAugmentData()      | 聚合符文数据（不保存）            | 否
 * aggregateAndSaveHeroData()  | 聚合并保存英雄数据                | 否
 * aggregateAndSaveAugmentData()| 聚合并保存符文数据               | 否
 * buildHeroFromRiotData()     | 从Riot数据构建英雄实体（私有）    | 否
 * extractSkills()             | 提取英雄技能数据（私有）          | 否
 * mergeAramStats()            | 合并ARAM统计数据到英雄（私有）    | 否
 * fillDefaultAramStats()      | 填充默认ARAM统计（私有）          | 否
 * buildAugmentFromAramStats() | 从ARAM数据构建符文实体（私有）    | 否
 * isValidAugmentStats()       | 验证符文数据有效性（私有）        | 否
 * clamp()                     | 数值范围限制（私有）              | 否
 * sanitizeTier()              | 段位清洗（私有）                  | 否
 * sanitizeQuality()           | 品质清洗（私有）                  | 否
 * capitalize()                | 首字母大写（私有）                | 否
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataAggregatorServiceImpl implements DataAggregatorService {

    /**
     * 胜率最小值 —— 0%
     */
    private static final BigDecimal WIN_RATE_MIN = BigDecimal.ZERO;

    /**
     * 胜率最大值 —— 100%
     */
    private static final BigDecimal WIN_RATE_MAX = new BigDecimal("100");

    /**
     * 选取率最小值 —— 0%
     */
    private static final BigDecimal PICK_RATE_MIN = BigDecimal.ZERO;

    /**
     * 选取率最大值 —— 100%
     */
    private static final BigDecimal PICK_RATE_MAX = new BigDecimal("100");

    /**
     * 平均排名最小值 —— 第1名
     */
    private static final BigDecimal AVG_PLACEMENT_MIN = BigDecimal.ONE;

    /**
     * 平均排名最大值 —— 第8名（8人游戏）
     */
    private static final BigDecimal AVG_PLACEMENT_MAX = new BigDecimal("8");

    /**
     * 默认胜率 —— 50%（无数据时的假设值）
     */
    private static final BigDecimal DEFAULT_WIN_RATE = new BigDecimal("50.00");

    /**
     * 默认选取率 —— 0%（无数据时假设没人选）
     */
    private static final BigDecimal DEFAULT_PICK_RATE = new BigDecimal("0.00");

    /**
     * 默认平均排名 —— 4.5（8人游戏的中位数）
     */
    private static final BigDecimal DEFAULT_AVG_PLACEMENT = new BigDecimal("4.50");

    /**
     * 默认段位 —— C（最低段位，无数据时保守估计）
     */
    private static final String DEFAULT_TIER = "C";

    /**
     * 默认置信度 —— low（低置信度，数据来自默认值而非实际统计）
     */
    private static final String DEFAULT_CONFIDENCE = "low";

    /**
     * Riot DataDragon 客户端 —— 获取英雄基础信息
     *
     * 提供的数据：英雄英文名、中文名、称号、头像URL、角色定位、技能列表
     */
    private final RiotDataDragonClient riotDataDragonClient;

    /**
     * ARAM 数据采集器 —— 获取 ARAM 模式的统计数据
     *
     * 提供的数据：胜率、选取率、段位评级、KDA、符文统计等
     */
    private final AramDataCollector aramDataCollector;

    /**
     * 英雄数据访问对象 —— 用于保存/更新英雄数据
     */
    private final HeroMapper heroMapper;

    /**
     * 符文数据访问对象 —— 用于保存/更新符文数据
     */
    private final AugmentMapper augmentMapper;

    /**
     * 聚合英雄数据 —— 从两个数据源合并英雄信息
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 从 Riot DataDragon 获取英雄基础信息，从 U.GG 获取 ARAM 统计数据，
     * 然后按英雄名称匹配合并，生成完整的 Hero 实体列表。
     *
     * @param version 游戏版本号，如 "14.1.1"，用于构建 DataDragon 的 URL
     * @return List<Hero> 聚合后的英雄列表（未保存到数据库）
     *
     * ══════════════════════════════════════════════════════════════
     * 执行流程
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 从 Riot DataDragon 获取英雄列表（名称、头像、技能等）
     * 2. 从 U.GG 获取 ARAM 统计数据（胜率、选取率等）
     * 3. 按英文名称匹配，将统计数据合并到英雄实体中
     * 4. 未匹配的英雄使用默认值填充
     * 5. 返回完整的英雄列表
     *
     * ══════════════════════════════════════════════════════════════
     * 匹配策略
     * ══════════════════════════════════════════════════════════════
     *
     * 使用英文名称（小写）作为匹配键：
     * - Riot DataDragon 的 champion ID（如 "Aatrox"）
     * - U.GG 的 championName（如 "Aatrox"）
     * 统一转小写后比较，避免大小写不匹配
     */
    @Override
    public List<Hero> aggregateHeroData(String version) {
        log.info("[AGGREGATE] hero data aggregation started | version={}", version);

        // ─── 第1步：从 Riot DataDragon 获取英雄基础信息 ───
        // 返回 Map<championKey, championData>，如 {"Aatrox": {...}, "Ahri": {...}}
        Map<String, JsonNode> championList = riotDataDragonClient.fetchChampionList(version);
        if (championList.isEmpty()) {
            log.warn("[AGGREGATE] RiotDataDragon champion list is empty, skipping hero aggregation");
            return Collections.emptyList();
        }
        log.info("[AGGREGATE] RiotDataDragon fetched {} champions", championList.size());

        // ─── 第2步：从 U.GG 获取 ARAM 统计数据 ───
        List<AramHeroStatsDTO> aramStats = aramDataCollector.collectAramStats();

        // 将列表转为 Map，以英文名称（小写）为键，方便快速查找
        // (existing, replacement) -> existing：如果出现重名，保留第一个
        Map<String, AramHeroStatsDTO> aramStatsMap = aramStats.stream()
                .collect(Collectors.toMap(
                        dto -> dto.getChampionName().toLowerCase(),
                        dto -> dto,
                        (existing, replacement) -> existing
                ));
        log.info("[AGGREGATE] AramDataCollector fetched {} hero stats", aramStats.size());

        // ─── 第3步：逐个合并英雄数据 ───
        List<Hero> heroes = new ArrayList<>();
        int matched = 0;    // 成功匹配ARAM数据的英雄数
        int unmatched = 0;  // 未匹配ARAM数据的英雄数

        for (Map.Entry<String, JsonNode> entry : championList.entrySet()) {
            String championKey = entry.getKey();
            JsonNode championData = entry.getValue();

            // 从 Riot 数据中提取英雄英文名
            String nameEn = championData.path("id").asText(championKey);

            // 在 ARAM 统计数据中查找对应的英雄
            AramHeroStatsDTO aramStat = aramStatsMap.get(nameEn.toLowerCase());

            // 从 Riot 数据构建英雄基础实体
            Hero hero = buildHeroFromRiotData(championData, version);

            if (aramStat != null) {
                // 找到了对应的 ARAM 统计数据，合并到英雄实体中
                mergeAramStats(hero, aramStat);
                matched++;
            } else {
                // 未找到 ARAM 统计数据，使用默认值填充
                fillDefaultAramStats(hero);
                unmatched++;
                log.debug("[AGGREGATE] no ARAM stats for champion={} | using defaults", nameEn);
            }

            heroes.add(hero);
        }

        log.info("[AGGREGATE] hero data aggregation completed | total={} | matched={} | unmatched={}",
                heroes.size(), matched, unmatched);
        return heroes;
    }

    /**
     * 聚合符文数据 —— 从 ARAM 数据源获取符文统计信息
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 与英雄聚合不同，符文数据只来自一个数据源（U.GG），
     * 不需要多源合并，只需要清洗和验证。
     *
     * @return List<Augment> 聚合后的符文列表（未保存到数据库）
     *
     * ══════════════════════════════════════════════════════════════
     * 为什么符文不需要多源合并？
     * ══════════════════════════════════════════════════════════════
     *
     * Riot DataDragon 不提供符文的 ARAM 统计数据，
     * 符文的胜率、选取率等数据只来自 U.GG 等第三方统计网站。
     */
    @Override
    public List<Augment> aggregateAugmentData() {
        log.info("[AGGREGATE] augment data aggregation started");

        // ─── 从 U.GG 获取符文统计数据 ───
        List<AramAugmentStatsDTO> aramStats = aramDataCollector.collectAugmentStats();
        if (aramStats.isEmpty()) {
            log.warn("[AGGREGATE] AramDataCollector augment stats is empty, skipping augment aggregation");
            return Collections.emptyList();
        }

        // ─── 逐个处理符文数据 ───
        List<Augment> augments = new ArrayList<>();
        int valid = 0;    // 有效数据数
        int invalid = 0;  // 无效数据数（被跳过）

        for (AramAugmentStatsDTO dto : aramStats) {
            // 验证数据有效性：名称不能为空，胜率和选取率不能超出范围
            if (!isValidAugmentStats(dto)) {
                invalid++;
                log.debug("[AGGREGATE] invalid augment stats skipped | name={} | winRate={} | pickRate={}",
                        dto.getAugmentName(), dto.getWinRate(), dto.getPickRate());
                continue;
            }

            // 从 ARAM 统计数据构建符文实体
            Augment augment = buildAugmentFromAramStats(dto);
            augments.add(augment);
            valid++;
        }

        log.info("[AGGREGATE] augment data aggregation completed | total={} | valid={} | invalid={}",
                aramStats.size(), valid, invalid);
        return augments;
    }

    /**
     * 聚合并保存英雄数据 —— 聚合后写入数据库
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 先调用 aggregateHeroData() 聚合数据，然后逐条保存到数据库。
     * 使用"存在则更新，不存在则插入"（Upsert）策略。
     *
     * @param version 游戏版本号
     * @return List<Hero> 保存后的英雄列表
     *
     * ══════════════════════════════════════════════════════════════
     * Upsert 策略说明
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 根据英文名称查询数据库
     * 2. 如果已存在 → 更新（保留原ID，更新其他字段）
     * 3. 如果不存在 → 插入新记录
     *
     * 这样可以保证重复执行数据同步时不会产生重复数据。
     */
    @Override
    public List<Hero> aggregateAndSaveHeroData(String version) {
        // 先聚合数据
        List<Hero> heroes = aggregateHeroData(version);
        if (heroes.isEmpty()) {
            log.warn("[UPSERT] no hero data to save");
            return heroes;
        }

        int inserted = 0;
        int updated = 0;

        for (Hero hero : heroes) {
            // 根据英文名称查询是否已存在
            Hero existing = heroMapper.selectOne(
                    new LambdaQueryWrapper<Hero>().eq(Hero::getNameEn, hero.getNameEn()));

            if (existing != null) {
                // 已存在 → 设置ID后更新
                hero.setId(existing.getId());
                heroMapper.updateById(hero);
                updated++;
            } else {
                // 不存在 → 插入新记录
                heroMapper.insert(hero);
                inserted++;
            }
        }

        log.info("[UPSERT] hero data saved | total={} | inserted={} | updated={}", heroes.size(), inserted, updated);
        return heroes;
    }

    /**
     * 聚合并保存符文数据 —— 聚合后写入数据库
     *
     * 与 aggregateAndSaveHeroData() 逻辑相同，只是操作的是符文数据。
     * 使用"存在则更新，不存在则插入"策略，以英文名称作为唯一标识。
     *
     * @return List<Augment> 保存后的符文列表
     */
    @Override
    public List<Augment> aggregateAndSaveAugmentData() {
        List<Augment> augments = aggregateAugmentData();
        if (augments.isEmpty()) {
            log.warn("[UPSERT] no augment data to save");
            return augments;
        }

        int inserted = 0;
        int updated = 0;

        for (Augment augment : augments) {
            Augment existing = augmentMapper.selectOne(
                    new LambdaQueryWrapper<Augment>().eq(Augment::getNameEn, augment.getNameEn()));

            if (existing != null) {
                augment.setId(existing.getId());
                augmentMapper.updateById(augment);
                updated++;
            } else {
                augmentMapper.insert(augment);
                inserted++;
            }
        }

        log.info("[UPSERT] augment data saved | total={} | inserted={} | updated={}", augments.size(), inserted, updated);
        return augments;
    }

    /**
     * 从 Riot DataDragon 数据构建英雄基础实体
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 解析 Riot DataDragon 返回的 JSON 数据，提取英雄的基础信息：
     * - 英文名（id）、中文名（name）、称号（title）
     * - 头像URL（拼接 DataDragon CDN 地址）
     * - 角色定位（tags 数组的第一个元素）
     * - 技能列表（被动 + Q/W/E/R）
     *
     * @param championData Riot DataDragon 返回的英雄 JSON 数据
     * @param version 游戏版本号，用于构建头像URL
     * @return Hero 英雄基础实体（不含 ARAM 统计数据）
     */
    private Hero buildHeroFromRiotData(JsonNode championData, String version) {
        Hero hero = new Hero();

        // ─── 提取基础信息 ───
        // .path() 不会抛异常，如果字段不存在返回 MissingNode
        // .asText("") 设置默认值，避免返回 null
        String id = championData.path("id").asText("");
        String name = championData.path("name").asText("");
        String title = championData.path("title").asText("");

        // ─── 构建头像URL ───
        // Riot DataDragon 的头像URL格式：
        // https://ddragon.leagueoflegends.com/cdn/{version}/img/champion/{imageFull}
        JsonNode image = championData.path("image");
        String imageFull = image.path("full").asText("");
        String imageUrl = imageFull.isEmpty() ? "" :
                String.format("%s/cdn/%s/img/champion/%s",
                        "https://ddragon.leagueoflegends.com", version, imageFull);

        // ─── 提取角色定位 ───
        // tags 是数组，如 ["Fighter", "Tank"]，取第一个作为主定位
        JsonNode tags = championData.path("tags");
        String role = tags.isArray() && !tags.isEmpty() ? tags.get(0).asText("") : "";

        // ─── 设置英雄属性 ───
        hero.setNameEn(id);           // 英文名使用 id 字段（如 "Aatrox"）
        hero.setNameZh(name);         // 中文名（如 "暗裔剑魔"）
        hero.setTitle(title);         // 称号（如 "暗裔剑魔"）
        hero.setImageUrl(imageUrl);   // 头像URL
        hero.setRole(capitalize(role)); // 角色定位（首字母大写）
        hero.setVersion(version);     // 游戏版本

        // ─── 提取技能列表 ───
        List<Hero.SkillData> skills = extractSkills(championData);
        hero.setSkills(skills);

        return hero;
    }

    /**
     * 提取英雄技能数据 —— 解析被动技能和Q/W/E/R技能
     *
     * ══════════════════════════════════════════════════════════════
     * 技能数据结构说明
     * ══════════════════════════════════════════════════════════════
     *
     * Riot DataDragon 的技能数据格式：
     * {
     *   "passive": { "name": "...", "description": "..." },  ← 被动技能
     *   "spells": [                                           ← 主动技能数组
     *     { "name": "...", "description": "..." },            ← Q技能
     *     { "name": "...", "description": "..." },            ← W技能
     *     { "name": "...", "description": "..." },            ← E技能
     *     { "name": "...", "description": "..." }             ← R技能
     *   ]
     * }
     *
     * @param championData 英雄 JSON 数据
     * @return List<Hero.SkillData> 技能列表，顺序为：被动、Q、W、E、R
     */
    private List<Hero.SkillData> extractSkills(JsonNode championData) {
        List<Hero.SkillData> skills = new ArrayList<>();

        // ─── 提取被动技能 ───
        JsonNode passive = championData.path("passive");
        if (!passive.isMissingNode()) {
            Hero.SkillData passiveSkill = new Hero.SkillData();
            passiveSkill.setKey("被动");
            passiveSkill.setName(passive.path("name").asText(""));
            passiveSkill.setDescription(passive.path("description").asText(""));
            skills.add(passiveSkill);
        }

        // ─── 提取主动技能（Q/W/E/R） ───
        JsonNode spells = championData.path("spells");
        if (spells.isArray()) {
            String[] keys = {"Q", "W", "E", "R"};
            for (int i = 0; i < Math.min(spells.size(), keys.length); i++) {
                JsonNode spell = spells.get(i);
                Hero.SkillData skill = new Hero.SkillData();
                skill.setKey(keys[i]);
                skill.setName(spell.path("name").asText(""));
                skill.setDescription(spell.path("description").asText(""));
                skills.add(skill);
            }
        }

        return skills;
    }

    /**
     * 合并 ARAM 统计数据到英雄实体
     *
     * 将 U.GG 的统计数据（胜率、选取率、段位等）合并到已构建的英雄实体中。
     * 合并后置信度设为 "high"（高），因为数据来自实际统计。
     *
     * @param hero 英雄实体（已有基础信息）
     * @param aramStat ARAM 统计数据
     */
    private void mergeAramStats(Hero hero, AramHeroStatsDTO aramStat) {
        hero.setTier(sanitizeTier(aramStat.getTier()));                           // 段位（清洗后）
        hero.setWinRate(clamp(aramStat.getWinRate(), WIN_RATE_MIN, WIN_RATE_MAX)); // 胜率（范围限制）
        hero.setPickRate(clamp(aramStat.getPickRate(), PICK_RATE_MIN, PICK_RATE_MAX)); // 选取率
        hero.setAvgKills(aramStat.getAvgKills());       // 场均击杀
        hero.setAvgDeaths(aramStat.getAvgDeaths());     // 场均死亡
        hero.setAvgAssists(aramStat.getAvgAssists());   // 场均助攻
        hero.setConfidenceLevel("high");                // 置信度：高（有实际数据）
    }

    /**
     * 填充默认 ARAM 统计数据
     *
     * 当英雄在 U.GG 中没有对应的 ARAM 统计数据时，使用默认值填充。
     * 置信度设为 "low"（低），因为数据是估计值而非实际统计。
     *
     * @param hero 英雄实体
     */
    private void fillDefaultAramStats(Hero hero) {
        hero.setTier(DEFAULT_TIER);               // 默认段位：C
        hero.setWinRate(DEFAULT_WIN_RATE);         // 默认胜率：50%
        hero.setPickRate(DEFAULT_PICK_RATE);       // 默认选取率：0%
        hero.setConfidenceLevel(DEFAULT_CONFIDENCE); // 默认置信度：low
    }

    /**
     * 从 ARAM 统计数据构建符文实体
     *
     * @param dto 符文统计数据
     * @return Augment 符文实体
     */
    private Augment buildAugmentFromAramStats(AramAugmentStatsDTO dto) {
        Augment augment = new Augment();
        augment.setNameEn(dto.getAugmentName());                                          // 符文英文名
        augment.setQuality(sanitizeQuality(dto.getQuality()));                            // 品质（清洗后）
        augment.setWinRate(clamp(dto.getWinRate(), WIN_RATE_MIN, WIN_RATE_MAX));          // 胜率
        augment.setPickRate(clamp(dto.getPickRate(), PICK_RATE_MIN, PICK_RATE_MAX));      // 选取率
        augment.setAvgPlacement(clamp(dto.getAvgPlacement(), AVG_PLACEMENT_MIN, AVG_PLACEMENT_MAX)); // 平均排名
        augment.setTier(sanitizeTier(dto.getTier()));                                     // 段位
        augment.setSynergySet(dto.getSynergySet());                                       // 套装名称
        return augment;
    }

    /**
     * 验证符文统计数据的有效性
     *
     * 检查规则：
     * 1. 名称不能为空或纯空格
     * 2. 胜率如果存在，必须在 0~100 之间
     * 3. 选取率如果存在，必须在 0~100 之间
     *
     * @param dto 符文统计数据
     * @return true=数据有效，false=数据无效
     */
    private boolean isValidAugmentStats(AramAugmentStatsDTO dto) {
        if (dto.getAugmentName() == null || dto.getAugmentName().isBlank()) {
            return false;
        }
        if (dto.getWinRate() != null && (dto.getWinRate().compareTo(WIN_RATE_MIN) < 0 || dto.getWinRate().compareTo(WIN_RATE_MAX) > 0)) {
            return false;
        }
        if (dto.getPickRate() != null && (dto.getPickRate().compareTo(PICK_RATE_MIN) < 0 || dto.getPickRate().compareTo(PICK_RATE_MAX) > 0)) {
            return false;
        }
        return true;
    }

    /**
     * 数值范围限制 —— 将值限制在 [min, max] 范围内
     *
     * 如果值小于 min，返回 min；如果值大于 max，返回 max；否则返回原值。
     * 如果值为 null，返回 min。
     *
     * @param value 要限制的值
     * @param min   最小值
     * @param max   最大值
     * @return 限制后的值
     */
    private BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) {
            return min;
        }
        if (value.compareTo(min) < 0) {
            log.trace("[CLEAN] clamped value {} to min {}", value, min);
            return min;
        }
        if (value.compareTo(max) > 0) {
            log.trace("[CLEAN] clamped value {} to max {}", value, max);
            return max;
        }
        return value;
    }

    /**
     * 段位清洗 —— 确保段位值在合法范围内
     *
     * 合法段位：S+、S、A、B、C
     * 其他值替换为默认段位 C
     *
     * @param tier 原始段位值
     * @return 清洗后的段位值
     */
    private String sanitizeTier(String tier) {
        if (tier == null || tier.isBlank()) {
            return DEFAULT_TIER;
        }
        String trimmed = tier.trim().toUpperCase();
        if (Set.of("S+", "S", "A", "B", "C").contains(trimmed)) {
            return trimmed;
        }
        log.debug("[CLEAN] unknown tier '{}' replaced with default '{}'", tier, DEFAULT_TIER);
        return DEFAULT_TIER;
    }

    /**
     * 品质清洗 —— 确保品质值在合法范围内，支持英文→中文映射
     *
     * 合法品质：银色、金色、棱彩
     * 英文映射：silver→银色、gold→金色、prismatic→棱彩
     * 其他值替换为默认品质"银色"
     *
     * @param quality 原始品质值
     * @return 清洗后的品质值
     */
    private String sanitizeQuality(String quality) {
        if (quality == null || quality.isBlank()) {
            return "银色";
        }
        String trimmed = quality.trim();
        if (Set.of("银色", "金色", "棱彩").contains(trimmed)) {
            return trimmed;
        }
        // 英文→中文映射
        Map<String, String> qualityMap = Map.of(
                "silver", "银色",
                "gold", "金色",
                "prismatic", "棱彩"
        );
        String mapped = qualityMap.get(trimmed.toLowerCase());
        if (mapped != null) {
            return mapped;
        }
        log.debug("[CLEAN] unknown quality '{}' replaced with default '银色'", quality);
        return "银色";
    }

    /**
     * 首字母大写 —— 将字符串的首字母转为大写，其余转为小写
     *
     * 例如："fighter" → "Fighter"、"TANK" → "Tank"
     *
     * @param str 原始字符串
     * @return 首字母大写的字符串
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
