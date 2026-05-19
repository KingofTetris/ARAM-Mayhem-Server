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

@Component
public class AugmentDataInitializer implements CommandLineRunner {

    private final AugmentMapper augmentMapper;

    public AugmentDataInitializer(AugmentMapper augmentMapper) {
        this.augmentMapper = augmentMapper;
    }

    private static final List<String> SYNERGY_SETS = List.of(
            "shield", "regeneration", "shield-break", "damage", "caster",
            "healing", "burst-healing", "slow", "attack-speed"
    );

    @Override
    @Transactional
    public void run(String... args) {
        if (augmentMapper.selectCount(null) > 0) {
            return;
        }

        List<Augment> augments = new ArrayList<>();
        int id = 1;

        augments.addAll(createPrismaticAugments(id, 41, 48));
        id = 49;
        augments.addAll(createLegendaryAugments(id, 49, 80));
        id = 81;
        augments.addAll(createEpicAugments(id, 81, 110));
        id = 111;
        augments.addAll(createMinorAugments(id, 111, 130));

        augments.forEach(augmentMapper::insert);
    }

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

    private Augment createAugment(int riotId, String nameZh, String nameEn, String description,
                                  String quality, String synergySet, String synergySet2, String synergySet3,
                                  double winRate, double pickRate, double avgPlacement, String tier) {
        Augment augment = new Augment();
        augment.setNameZh(nameZh);
        augment.setNameEn(nameEn);
        augment.setDescription(description);
        augment.setQuality(quality);
        augment.setSynergySet(synergySet);
        augment.setSynergySet2(synergySet2);
        augment.setSynergySet3(synergySet3);
        augment.setIconUrl("/images/augments/" + nameEn.toLowerCase().trim().replace(" ", "_") + ".png");
        augment.setWinRate(BigDecimal.valueOf(winRate));
        augment.setPickRate(BigDecimal.valueOf(pickRate));
        augment.setAvgPlacement(BigDecimal.valueOf(avgPlacement));
        augment.setTier(tier);
        augment.setIsTrap(false);
        augment.setVersion("14.10");
        augment.setUpdatedAt(LocalDateTime.now());
        return augment;
    }
}
