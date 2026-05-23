package com.aram.mayhem.initializer;

import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.mapper.AugmentMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 强化符文数据初始化器 —— 应用启动时的"符文种子数据播种机"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 与 DataInitializer 类似，本类实现了 CommandLineRunner 接口，
 * 在应用启动完成后自动执行，检查 tb_augment 表是否为空，如果为空则插入种子符文数据。
 *
 * 打个比方：
 * - DataInitializer 是"英雄播种机"（播下英雄种子）
 * - 本类是"符文播种机"（播下符文种子）
 * - BulletinDataInitializer 是"公告播种机"（播下公告种子）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、符文品质分级
 * ═══════════════════════════════════════════════════════════════════
 *
 * ARAM 模式中的强化符文按品质分为4个等级：
 *
 * 品质         | 英文名      | 梯级  | 数量  | 说明
 * ------------|------------|-------|------|---------------------------
 * 棱彩         | Prismatic  | T1    | 8    | 最强符文，出场即改变战局
 * 传说         | Legendary  | T2    | 10   | 强力符文，显著增强某方面能力
 * 史诗         | Epic       | T3    | 10   | 中等符文，提供特定属性加成
 * 基础         | Minor      | T3    | 10   | 基础符文，小幅属性提升
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、符文套装系统
 * ═══════════════════════════════════════════════════════════════════
 *
 * 每个符文可以属于1~3个套装（synergySet/synergySet2/synergySet3）：
 * - shield：护盾套装
 * - regeneration：回复套装
 * - damage：伤害套装
 * - caster：施法套装
 * - healing：治疗套装
 * - burst-healing：爆发治疗套装
 * - slow：减速套装
 * - attack-speed：攻速套装
 * - shield-break：破盾套装
 *
 * 当玩家收集到同一套装的多个符文时，会触发套装效果。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、幂等性设计
 * ═══════════════════════════════════════════════════════════════════
 *
 * 与 DataInitializer 相同，本类也是幂等的：
 * - 第一次启动：tb_augment 为空 → 插入种子数据
 * - 第二次启动：tb_augment 不为空 → 跳过插入
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、关联类
 * ═══════════════════════════════════════════════════════════════════
 *
 * - Augment → 符文实体类，对应 tb_augment 表
 * - AugmentMapper → MyBatis-Plus Mapper，提供 CRUD 操作
 * - DataInitializer → 英雄数据初始化器
 * - BulletinDataInitializer → 公告数据初始化器
 */
@Component
public class AugmentDataInitializer implements CommandLineRunner {

    /**
     * AugmentMapper —— MyBatis-Plus 提供的符文表 CRUD 操作接口
     *
     * 主要使用的方法：
     * - selectCount(null)：查询表中总记录数
     * - insert(augment)：插入一条符文记录
     */
    private final AugmentMapper augmentMapper;

    /**
     * 构造函数 —— Spring 自动注入 AugmentMapper
     *
     * @param augmentMapper 符文表 Mapper
     */
    public AugmentDataInitializer(AugmentMapper augmentMapper) {
        this.augmentMapper = augmentMapper;
    }

    /**
     * 符文套装标识列表
     *
     * 定义了所有可用的套装类型，用于为符文分配套装归属。
     * 每个符文可以属于1~3个套装（通过 synergySet/synergySet2/synergySet3 字段）。
     */
    private static final List<String> SYNERGY_SETS = List.of(
            "shield", "regeneration", "shield-break", "damage", "caster",
            "healing", "burst-healing", "slow", "attack-speed"
    );

    /**
     * 应用启动后自动执行 —— 检查并初始化符文种子数据
     *
     * ══════════════════════════════════════════════════════════════
     * 执行流程
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 检查 tb_augment 表是否已有数据
     * 2. 如果已有数据 → 直接返回（幂等性保证）
     * 3. 如果没有数据 → 按品质创建符文：
     *    a. 棱彩符文（8个）—— 最强品质
     *    b. 传说符文（10个）—— 强力品质
     *    c. 史诗符文（10个）—— 中等品质
     *    d. 基础符文（10个）—— 基础品质
     * 4. 逐条插入到数据库
     *
     * @param args Spring Boot 启动参数（本类不使用）
     */
    @Override
    @Transactional
    public void run(String... args) {
        // 幂等性检查：如果表中已有数据，跳过初始化
        if (augmentMapper.selectCount(null) > 0) {
            return;
        }

        List<Augment> augments = new ArrayList<>();
        int id = 1;

        // 按品质创建符文数据，每种品质调用不同的创建方法
        augments.addAll(createPrismaticAugments(id, 41, 48));     // 棱彩符文：8个
        id = 49;
        augments.addAll(createLegendaryAugments(id, 49, 80));     // 传说符文：10个
        id = 81;
        augments.addAll(createEpicAugments(id, 81, 110));         // 史诗符文：10个
        id = 111;
        augments.addAll(createMinorAugments(id, 111, 130));       // 基础符文：10个

        // 逐条插入到 tb_augment 表
        augments.forEach(augmentMapper::insert);
    }

    /**
     * 创建棱彩品质符文（最强品质）
     *
     * 棱彩符文是 ARAM 中最强的强化符文，胜率通常在 54%~58% 之间。
     * 它们能显著改变游戏局势，是优先选择的目标。
     *
     * @param startId       起始 Riot ID
     * @param endId         结束 Riot ID（当前未使用，预留扩展）
     * @param synergyCount  套装数量（当前未使用，预留扩展）
     * @return 棱彩符文列表
     */
    private List<Augment> createPrismaticAugments(int startId, int endId, int synergyCount) {
        List<Augment> augments = new ArrayList<>();
        int currentId = startId;

        augments.add(createAugment(currentId++, "寒凛亡息", "Eternal Winter", "周期性脉冲对敌人造成魔法伤害",
                "prismatic", "damage", "caster", "slow", 58.2, 12.5, 2.1, "T1"));
        augments.add(createAugment(currentId++, "荆棘之甲", "Thorns", "受到攻击时反弹伤害",
                "prismatic", "shield", null, null, 56.8, 8.3, 2.3, "T1"));
        augments.add(createAugment(currentId++, "生命汲取", "Life Drain", "造成伤害时恢复生命值",
                "prismatic", "healing", "regeneration", null, 57.5, 10.2, 2.2, "T1"));
        augments.add(createAugment(currentId++, "致命冲击", "Lethality", "物理伤害增加",
                "prismatic", "damage", "attack-speed", null, 55.9, 7.8, 2.5, "T1"));
        augments.add(createAugment(currentId++, "魔源泉涌", "Mana Surge", "技能伤害增加",
                "prismatic", "caster", "burst-healing", null, 56.3, 9.1, 2.4, "T1"));
        augments.add(createAugment(currentId++, "坚不可摧", "Unbreakable", "受到伤害减少",
                "prismatic", "shield", "regeneration", null, 55.1, 6.5, 2.6, "T1"));
        augments.add(createAugment(currentId++, "爆发治疗", "Burst Heal", "治疗效果翻倍",
                "prismatic", "healing", "burst-healing", null, 54.7, 5.9, 2.7, "T1"));
        augments.add(createAugment(currentId++, "灵魂熔炉", "Soul Furnace", "护盾和韧性强",
                "prismatic", "shield", "attack-speed", null, 54.2, 5.2, 2.8, "T1"));

        return augments;
    }

    /**
     * 创建传说品质符文（强力品质）
     *
     * 传说符文是第二强的品质，胜率通常在 49%~54% 之间。
     * 它们能显著增强某一方面的能力。
     *
     * @param startId       起始 Riot ID
     * @param endId         结束 Riot ID（当前未使用）
     * @param synergyCount  套装数量（当前未使用）
     * @return 传说符文列表
     */
    private List<Augment> createLegendaryAugments(int startId, int endId, int synergyCount) {
        List<Augment> augments = new ArrayList<>();
        int currentId = startId;

        augments.add(createAugment(currentId++, "迅捷步伐", "Swift Strikes", "攻击速度提升",
                "legendary", "attack-speed", "damage", null, 53.8, 18.5, 3.2, "T2"));
        augments.add(createAugment(currentId++, "法力具现", "Mana Font", "最大法力值提升",
                "legendary", "caster", "mana", null, 52.5, 15.3, 3.5, "T2"));
        augments.add(createAugment(currentId++, "活力涌动", "Vitality Flow", "生命恢复速度提升",
                "legendary", "regeneration", "healing", null, 51.9, 14.1, 3.6, "T2"));
        augments.add(createAugment(currentId++, "护盾强化", "Shield Boost", "护盾效果提升",
                "legendary", "shield", "healing", null, 52.1, 13.8, 3.5, "T2"));
        augments.add(createAugment(currentId++, "穿甲", "Armor Shred", "护甲穿透",
                "legendary", "damage", "attack-speed", null, 51.5, 12.6, 3.7, "T2"));
        augments.add(createAugment(currentId++, "魔抗穿透", "Magic Pen", "魔法穿透",
                "legendary", "caster", "damage", null, 51.2, 11.9, 3.8, "T2"));
        augments.add(createAugment(currentId++, "减速强化", "Slow Enhance", "减速效果增强",
                "legendary", "slow", "damage", null, 50.8, 10.5, 3.9, "T2"));
        augments.add(createAugment(currentId++, "治疗强化", "Heal Boost", "治疗效果提升",
                "legendary", "healing", "burst-healing", null, 50.5, 9.8, 4.0, "T2"));
        augments.add(createAugment(currentId++, "韧性强化", "Tenacity Boost", "受到控制时间减少",
                "legendary", "shield", "regeneration", null, 50.2, 8.5, 4.1, "T2"));
        augments.add(createAugment(currentId++, "暴击强化", "Crit Boost", "暴击率提升",
                "legendary", "damage", "attack-speed", null, 49.8, 7.9, 4.2, "T2"));

        return augments;
    }

    /**
     * 创建史诗品质符文（中等品质）
     *
     * 史诗符文提供特定属性加成，胜率通常在 46%~50% 之间。
     *
     * @param startId       起始 Riot ID
     * @param endId         结束 Riot ID（当前未使用）
     * @param synergyCount  套装数量（当前未使用）
     * @return 史诗符文列表
     */
    private List<Augment> createEpicAugments(int startId, int endId, int synergyCount) {
        List<Augment> augments = new ArrayList<>();
        int currentId = startId;

        augments.add(createAugment(currentId++, "生命值提升 I", "HP Boost I", "最大生命值提升",
                "epic", "shield", "regeneration", null, 49.5, 22.1, 4.3, "T3"));
        augments.add(createAugment(currentId++, "攻击力提升 I", "AD Boost I", "物理攻击力提升",
                "epic", "damage", "attack-speed", null, 49.2, 21.5, 4.4, "T3"));
        augments.add(createAugment(currentId++, "法术强度提升 I", "AP Boost I", "魔法伤害提升",
                "epic", "caster", "damage", null, 48.9, 20.8, 4.5, "T3"));
        augments.add(createAugment(currentId++, "护甲提升 I", "Armor Boost I", "护甲提升",
                "epic", "shield", "shield-break", null, 48.6, 19.5, 4.6, "T3"));
        augments.add(createAugment(currentId++, "魔抗提升 I", "MR Boost I", "魔法抗性提升",
                "epic", "shield", "regeneration", null, 48.3, 18.2, 4.7, "T3"));
        augments.add(createAugment(currentId++, "技能急速 I", "CDR Boost I", "技能冷却减少",
                "epic", "caster", "damage", null, 48.0, 17.1, 4.8, "T3"));
        augments.add(createAugment(currentId++, "生命偷取 I", "Lifesteal I", "物理生命偷取",
                "epic", "damage", "healing", null, 47.7, 15.8, 4.9, "T3"));
        augments.add(createAugment(currentId++, "法术吸血 I", "Spellvamp I", "法术吸血",
                "epic", "caster", "healing", null, 47.4, 14.5, 5.0, "T3"));
        augments.add(createAugment(currentId++, "移速提升 I", "MS Boost I", "移动速度提升",
                "epic", "attack-speed", "damage", null, 47.1, 13.2, 5.1, "T3"));
        augments.add(createAugment(currentId++, "韧性 I", "Tenacity I", "控制减免",
                "epic", "shield", "regeneration", null, 46.8, 12.1, 5.2, "T3"));

        return augments;
    }

    /**
     * 创建基础品质符文（最低品质）
     *
     * 基础符文提供小幅属性提升，胜率通常在 43%~47% 之间。
     * 注意：种子数据中基础符文的 quality 字段也设为 "epic"，
     * 这是数据简化处理，真实数据由 DataSyncScheduler 同步。
     *
     * @param startId       起始 Riot ID
     * @param endId         结束 Riot ID（当前未使用）
     * @param synergyCount  套装数量（当前未使用）
     * @return 基础符文列表
     */
    private List<Augment> createMinorAugments(int startId, int endId, int synergyCount) {
        List<Augment> augments = new ArrayList<>();
        int currentId = startId;

        augments.add(createAugment(currentId++, "小型生命值", "Small HP", "生命值少量提升",
                "epic", "shield", null, null, 46.5, 25.3, 5.3, "T3"));
        augments.add(createAugment(currentId++, "小型攻击力", "Small AD", "攻击力少量提升",
                "epic", "damage", null, null, 46.2, 24.8, 5.4, "T3"));
        augments.add(createAugment(currentId++, "小型护甲", "Small Armor", "护甲少量提升",
                "epic", "shield", "shield-break", null, 45.9, 23.5, 5.5, "T3"));
        augments.add(createAugment(currentId++, "小型魔抗", "Small MR", "魔抗少量提升",
                "epic", "shield", "regeneration", null, 45.6, 22.2, 5.6, "T3"));
        augments.add(createAugment(currentId++, "小型技能急速", "Small CDR", "冷却少量减少",
                "epic", "caster", "damage", null, 45.3, 20.9, 5.7, "T3"));
        augments.add(createAugment(currentId++, "小型移速", "Small MS", "移速少量提升",
                "epic", "attack-speed", null, null, 45.0, 19.6, 5.8, "T3"));
        augments.add(createAugment(currentId++, "小型生命恢复", "Small Regen", "生命恢复少量提升",
                "epic", "regeneration", "healing", null, 44.7, 18.3, 5.9, "T3"));
        augments.add(createAugment(currentId++, "小型护盾", "Small Shield", "护盾效果少量提升",
                "epic", "shield", "healing", null, 44.4, 17.0, 6.0, "T3"));
        augments.add(createAugment(currentId++, "小型减速", "Small Slow", "减速效果少量增强",
                "epic", "slow", "damage", null, 44.1, 15.7, 6.1, "T3"));
        augments.add(createAugment(currentId++, "小型暴击", "Small Crit", "暴击率少量提升",
                "epic", "damage", "attack-speed", null, 43.8, 14.4, 6.2, "T3"));

        return augments;
    }

    /**
     * 创建单个符文实体 —— 种子数据的工厂方法
     *
     * ══════════════════════════════════════════════════════════════
     * 参数说明
     * ══════════════════════════════════════════════════════════════
     *
     * @param riotId        Riot 官方符文 ID
     * @param nameZh        中文名（如 "寒凛亡息"），用于前端显示
     * @param nameEn        英文名（如 "Eternal Winter"），用于拼接图片 URL
     * @param description   符文效果描述
     * @param quality       品质（prismatic/legendary/epic）
     * @param synergySet    主套装标识（如 "damage"），不可为 null
     * @param synergySet2   第二套装标识，可为 null（表示不属于第二套装）
     * @param synergySet3   第三套装标识，可为 null
     * @param winRate       胜率（如 58.2 表示 58.2%）
     * @param pickRate      选取率（如 12.5 表示 12.5%）
     * @param avgPlacement  平均排名（如 2.1，越低越好）
     * @param tier          梯级评级（T1/T2/T3）
     * @return 完整的 Augment 实体对象
     */
    private Augment createAugment(int riotId, String nameZh, String nameEn, String description,
                                  String quality, String synergySet, String synergySet2, String synergySet3,
                                  double winRate, double pickRate, double avgPlacement, String tier) {
        Augment augment = new Augment();
        augment.setNameZh(nameZh);                                                              // 中文名
        augment.setNameEn(nameEn);                                                              // 英文名
        augment.setDescription(description);                                                    // 效果描述
        augment.setQuality(quality);                                                            // 品质
        augment.setSynergySet(synergySet);                                                      // 主套装
        augment.setSynergySet2(synergySet2);                                                    // 第二套装（可为null）
        augment.setSynergySet3(synergySet3);                                                    // 第三套装（可为null）
        augment.setIconUrl("/images/augments/" + nameEn.toLowerCase().trim().replace(" ", "_") + ".png");  // 图片URL
        augment.setWinRate(BigDecimal.valueOf(winRate));                                        // 胜率
        augment.setPickRate(BigDecimal.valueOf(pickRate));                                      // 选取率
        augment.setAvgPlacement(BigDecimal.valueOf(avgPlacement));                              // 平均排名
        augment.setTier(tier);                                                                  // 梯级
        augment.setIsTrap(false);                                                               // 是否陷阱符文（种子数据全部为false）
        augment.setVersion("14.10");                                                            // 数据版本号
        augment.setUpdatedAt(LocalDateTime.now());                                              // 更新时间
        return augment;
    }
}
