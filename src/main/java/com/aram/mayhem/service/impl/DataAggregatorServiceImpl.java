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

@Slf4j
@Service
@RequiredArgsConstructor
public class DataAggregatorServiceImpl implements DataAggregatorService {

    private static final BigDecimal WIN_RATE_MIN = BigDecimal.ZERO;
    private static final BigDecimal WIN_RATE_MAX = new BigDecimal("100");
    private static final BigDecimal PICK_RATE_MIN = BigDecimal.ZERO;
    private static final BigDecimal PICK_RATE_MAX = new BigDecimal("100");
    private static final BigDecimal AVG_PLACEMENT_MIN = BigDecimal.ONE;
    private static final BigDecimal AVG_PLACEMENT_MAX = new BigDecimal("8");
    private static final BigDecimal DEFAULT_WIN_RATE = new BigDecimal("50.00");
    private static final BigDecimal DEFAULT_PICK_RATE = new BigDecimal("0.00");
    private static final BigDecimal DEFAULT_AVG_PLACEMENT = new BigDecimal("4.50");
    private static final String DEFAULT_TIER = "C";
    private static final String DEFAULT_CONFIDENCE = "low";

    private final RiotDataDragonClient riotDataDragonClient;
    private final AramDataCollector aramDataCollector;
    private final HeroMapper heroMapper;
    private final AugmentMapper augmentMapper;

    @Override
    public List<Hero> aggregateHeroData(String version) {
        log.info("[AGGREGATE] hero data aggregation started | version={}", version);

        Map<String, JsonNode> championList = riotDataDragonClient.fetchChampionList(version);
        if (championList.isEmpty()) {
            log.warn("[AGGREGATE] RiotDataDragon champion list is empty, skipping hero aggregation");
            return Collections.emptyList();
        }
        log.info("[AGGREGATE] RiotDataDragon fetched {} champions", championList.size());

        List<AramHeroStatsDTO> aramStats = aramDataCollector.collectAramStats();
        Map<String, AramHeroStatsDTO> aramStatsMap = aramStats.stream()
                .collect(Collectors.toMap(
                        dto -> dto.getChampionName().toLowerCase(),
                        dto -> dto,
                        (existing, replacement) -> existing
                ));
        log.info("[AGGREGATE] AramDataCollector fetched {} hero stats", aramStats.size());

        List<Hero> heroes = new ArrayList<>();
        int matched = 0;
        int unmatched = 0;

        for (Map.Entry<String, JsonNode> entry : championList.entrySet()) {
            String championKey = entry.getKey();
            JsonNode championData = entry.getValue();

            String nameEn = championData.path("id").asText(championKey);
            AramHeroStatsDTO aramStat = aramStatsMap.get(nameEn.toLowerCase());

            Hero hero = buildHeroFromRiotData(championData, version);

            if (aramStat != null) {
                mergeAramStats(hero, aramStat);
                matched++;
            } else {
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

    @Override
    public List<Augment> aggregateAugmentData() {
        log.info("[AGGREGATE] augment data aggregation started");

        List<AramAugmentStatsDTO> aramStats = aramDataCollector.collectAugmentStats();
        if (aramStats.isEmpty()) {
            log.warn("[AGGREGATE] AramDataCollector augment stats is empty, skipping augment aggregation");
            return Collections.emptyList();
        }

        List<Augment> augments = new ArrayList<>();
        int valid = 0;
        int invalid = 0;

        for (AramAugmentStatsDTO dto : aramStats) {
            if (!isValidAugmentStats(dto)) {
                invalid++;
                log.debug("[AGGREGATE] invalid augment stats skipped | name={} | winRate={} | pickRate={}",
                        dto.getAugmentName(), dto.getWinRate(), dto.getPickRate());
                continue;
            }

            Augment augment = buildAugmentFromAramStats(dto);
            augments.add(augment);
            valid++;
        }

        log.info("[AGGREGATE] augment data aggregation completed | total={} | valid={} | invalid={}",
                aramStats.size(), valid, invalid);
        return augments;
    }

    @Override
    public List<Hero> aggregateAndSaveHeroData(String version) {
        List<Hero> heroes = aggregateHeroData(version);
        if (heroes.isEmpty()) {
            log.warn("[UPSERT] no hero data to save");
            return heroes;
        }

        int inserted = 0;
        int updated = 0;

        for (Hero hero : heroes) {
            Hero existing = heroMapper.selectOne(
                    new LambdaQueryWrapper<Hero>().eq(Hero::getNameEn, hero.getNameEn()));

            if (existing != null) {
                hero.setId(existing.getId());
                heroMapper.updateById(hero);
                updated++;
            } else {
                heroMapper.insert(hero);
                inserted++;
            }
        }

        log.info("[UPSERT] hero data saved | total={} | inserted={} | updated={}", heroes.size(), inserted, updated);
        return heroes;
    }

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

    private Hero buildHeroFromRiotData(JsonNode championData, String version) {
        Hero hero = new Hero();

        String id = championData.path("id").asText("");
        String name = championData.path("name").asText("");
        String title = championData.path("title").asText("");

        JsonNode image = championData.path("image");
        String imageFull = image.path("full").asText("");
        String imageUrl = imageFull.isEmpty() ? "" :
                String.format("%s/cdn/%s/img/champion/%s",
                        "https://ddragon.leagueoflegends.com", version, imageFull);

        JsonNode tags = championData.path("tags");
        String role = tags.isArray() && !tags.isEmpty() ? tags.get(0).asText("") : "";

        hero.setNameEn(id);
        hero.setNameZh(name);
        hero.setTitle(title);
        hero.setImageUrl(imageUrl);
        hero.setRole(capitalize(role));
        hero.setVersion(version);

        List<Hero.SkillData> skills = extractSkills(championData);
        hero.setSkills(skills);

        return hero;
    }

    private List<Hero.SkillData> extractSkills(JsonNode championData) {
        List<Hero.SkillData> skills = new ArrayList<>();

        JsonNode passive = championData.path("passive");
        if (!passive.isMissingNode()) {
            Hero.SkillData passiveSkill = new Hero.SkillData();
            passiveSkill.setKey("被动");
            passiveSkill.setName(passive.path("name").asText(""));
            passiveSkill.setDescription(passive.path("description").asText(""));
            skills.add(passiveSkill);
        }

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

    private void mergeAramStats(Hero hero, AramHeroStatsDTO aramStat) {
        hero.setTier(sanitizeTier(aramStat.getTier()));
        hero.setWinRate(clamp(aramStat.getWinRate(), WIN_RATE_MIN, WIN_RATE_MAX));
        hero.setPickRate(clamp(aramStat.getPickRate(), PICK_RATE_MIN, PICK_RATE_MAX));
        hero.setAvgKills(aramStat.getAvgKills());
        hero.setAvgDeaths(aramStat.getAvgDeaths());
        hero.setAvgAssists(aramStat.getAvgAssists());
        hero.setConfidenceLevel("high");
    }

    private void fillDefaultAramStats(Hero hero) {
        hero.setTier(DEFAULT_TIER);
        hero.setWinRate(DEFAULT_WIN_RATE);
        hero.setPickRate(DEFAULT_PICK_RATE);
        hero.setConfidenceLevel(DEFAULT_CONFIDENCE);
    }

    private Augment buildAugmentFromAramStats(AramAugmentStatsDTO dto) {
        Augment augment = new Augment();
        augment.setNameEn(dto.getAugmentName());
        augment.setQuality(sanitizeQuality(dto.getQuality()));
        augment.setWinRate(clamp(dto.getWinRate(), WIN_RATE_MIN, WIN_RATE_MAX));
        augment.setPickRate(clamp(dto.getPickRate(), PICK_RATE_MIN, PICK_RATE_MAX));
        augment.setAvgPlacement(clamp(dto.getAvgPlacement(), AVG_PLACEMENT_MIN, AVG_PLACEMENT_MAX));
        augment.setTier(sanitizeTier(dto.getTier()));
        augment.setSynergySet(dto.getSynergySet());
        return augment;
    }

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

    private String sanitizeQuality(String quality) {
        if (quality == null || quality.isBlank()) {
            return "银色";
        }
        String trimmed = quality.trim();
        if (Set.of("银色", "金色", "棱彩").contains(trimmed)) {
            return trimmed;
        }
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

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
