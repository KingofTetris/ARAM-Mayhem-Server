package com.aram.mayhem.service.impl;

import com.aram.mayhem.client.AramDataCollector;
import com.aram.mayhem.client.RiotDataDragonClient;
import com.aram.mayhem.dto.AramAugmentStatsDTO;
import com.aram.mayhem.dto.AramHeroStatsDTO;
import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.mapper.AugmentMapper;
import com.aram.mayhem.mapper.HeroMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataAggregatorServiceImplTest {

    @Mock
    private RiotDataDragonClient riotDataDragonClient;

    @Mock
    private AramDataCollector aramDataCollector;

    @Mock
    private HeroMapper heroMapper;

    @Mock
    private AugmentMapper augmentMapper;

    private DataAggregatorServiceImpl service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new DataAggregatorServiceImpl(riotDataDragonClient, aramDataCollector, heroMapper, augmentMapper);
    }

    private ObjectNode buildChampionJson(String id, String name, String title) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", id);
        node.put("name", name);
        node.put("title", title);

        ObjectNode image = node.putObject("image");
        image.put("full", id + ".png");

        ArrayNode tags = node.putArray("tags");
        tags.add("fighter");

        ObjectNode passive = node.putObject("passive");
        passive.put("name", "被动技能");
        passive.put("description", "被动描述");

        ArrayNode spells = node.putArray("spells");
        for (String key : new String[]{"Q技能", "W技能", "E技能", "R技能"}) {
            ObjectNode spell = spells.addObject();
            spell.put("name", key);
            spell.put("description", key + "描述");
        }

        return node;
    }

    private AramHeroStatsDTO buildAramHeroStats(String name, String tier, double winRate, double pickRate) {
        return AramHeroStatsDTO.builder()
                .championName(name)
                .tier(tier)
                .winRate(BigDecimal.valueOf(winRate))
                .pickRate(BigDecimal.valueOf(pickRate))
                .avgKills(BigDecimal.valueOf(5.0))
                .avgDeaths(BigDecimal.valueOf(4.0))
                .avgAssists(BigDecimal.valueOf(8.0))
                .source("u.gg")
                .build();
    }

    private AramAugmentStatsDTO buildAramAugmentStats(String name, String quality, double winRate, double pickRate) {
        return AramAugmentStatsDTO.builder()
                .augmentName(name)
                .quality(quality)
                .winRate(BigDecimal.valueOf(winRate))
                .pickRate(BigDecimal.valueOf(pickRate))
                .avgPlacement(BigDecimal.valueOf(3.5))
                .tier("S")
                .synergySet("精密")
                .source("u.gg")
                .build();
    }

    @Nested
    @DisplayName("aggregateHeroData 测试")
    class AggregateHeroDataTest {

        @Test
        @DisplayName("正常聚合：RiotDataDragon + AramDataCollector 数据合并")
        void shouldMergeDataFromBothSources() {
            ObjectNode aatrox = buildChampionJson("Aatrox", "亚托克斯", "暗裔剑魔");
            ObjectNode ahri = buildChampionJson("Ahri", "阿狸", "九尾妖狐");
            when(riotDataDragonClient.fetchChampionList("14.10.1"))
                    .thenReturn(Map.of("Aatrox", aatrox, "Ahri", ahri));

            List<AramHeroStatsDTO> aramStats = List.of(
                    buildAramHeroStats("Aatrox", "S", 53.5, 12.0),
                    buildAramHeroStats("Ahri", "A", 51.2, 8.5)
            );
            when(aramDataCollector.collectAramStats()).thenReturn(aramStats);

            List<Hero> result = service.aggregateHeroData("14.10.1");

            assertEquals(2, result.size());

            Hero aatroxHero = result.stream().filter(h -> "Aatrox".equals(h.getNameEn())).findFirst().orElseThrow();
            assertEquals("亚托克斯", aatroxHero.getNameZh());
            assertEquals("暗裔剑魔", aatroxHero.getTitle());
            assertEquals("S", aatroxHero.getTier());
            assertEquals(new BigDecimal("53.5"), aatroxHero.getWinRate());
            assertEquals(new BigDecimal("12.0"), aatroxHero.getPickRate());
            assertEquals("high", aatroxHero.getConfidenceLevel());
            assertEquals("14.10.1", aatroxHero.getVersion());
            assertNotNull(aatroxHero.getSkills());
            assertEquals(5, aatroxHero.getSkills().size());
        }

        @Test
        @DisplayName("部分匹配：未匹配英雄填充默认值")
        void shouldFillDefaultsForUnmatchedChampions() {
            ObjectNode aatrox = buildChampionJson("Aatrox", "亚托克斯", "暗裔剑魔");
            when(riotDataDragonClient.fetchChampionList("14.10.1"))
                    .thenReturn(Map.of("Aatrox", aatrox));

            when(aramDataCollector.collectAramStats()).thenReturn(Collections.emptyList());

            List<Hero> result = service.aggregateHeroData("14.10.1");

            assertEquals(1, result.size());
            Hero hero = result.get(0);
            assertEquals("C", hero.getTier());
            assertEquals(new BigDecimal("50.00"), hero.getWinRate());
            assertEquals(new BigDecimal("0.00"), hero.getPickRate());
            assertEquals("low", hero.getConfidenceLevel());
        }

        @Test
        @DisplayName("RiotDataDragon 返回空列表")
        void shouldReturnEmptyWhenRiotDataEmpty() {
            when(riotDataDragonClient.fetchChampionList("14.10.1")).thenReturn(Collections.emptyMap());

            List<Hero> result = service.aggregateHeroData("14.10.1");

            assertTrue(result.isEmpty());
            verify(aramDataCollector, never()).collectAramStats();
        }

        @Test
        @DisplayName("胜率超出范围时被 clamp")
        void shouldClampOutOfRangeWinRate() {
            ObjectNode aatrox = buildChampionJson("Aatrox", "亚托克斯", "暗裔剑魔");
            when(riotDataDragonClient.fetchChampionList("14.10.1"))
                    .thenReturn(Map.of("Aatrox", aatrox));

            AramHeroStatsDTO invalidStats = AramHeroStatsDTO.builder()
                    .championName("Aatrox")
                    .tier("S")
                    .winRate(new BigDecimal("150.0"))
                    .pickRate(new BigDecimal("-5.0"))
                    .source("u.gg")
                    .build();
            when(aramDataCollector.collectAramStats()).thenReturn(List.of(invalidStats));

            List<Hero> result = service.aggregateHeroData("14.10.1");

            Hero hero = result.get(0);
            assertEquals(new BigDecimal("100"), hero.getWinRate());
            assertEquals(BigDecimal.ZERO, hero.getPickRate());
        }

        @Test
        @DisplayName("无效 tier 值被替换为默认 C")
        void shouldReplaceInvalidTierWithDefault() {
            ObjectNode aatrox = buildChampionJson("Aatrox", "亚托克斯", "暗裔剑魔");
            when(riotDataDragonClient.fetchChampionList("14.10.1"))
                    .thenReturn(Map.of("Aatrox", aatrox));

            AramHeroStatsDTO invalidTier = buildAramHeroStats("Aatrox", "Z", 50.0, 10.0);
            when(aramDataCollector.collectAramStats()).thenReturn(List.of(invalidTier));

            List<Hero> result = service.aggregateHeroData("14.10.1");

            assertEquals("C", result.get(0).getTier());
        }

        @Test
        @DisplayName("大小写不敏感匹配：championName 大小写无关")
        void shouldMatchCaseInsensitive() {
            ObjectNode aatrox = buildChampionJson("Aatrox", "亚托克斯", "暗裔剑魔");
            when(riotDataDragonClient.fetchChampionList("14.10.1"))
                    .thenReturn(Map.of("Aatrox", aatrox));

            AramHeroStatsDTO lowerCaseName = buildAramHeroStats("aatrox", "A", 51.0, 10.0);
            when(aramDataCollector.collectAramStats()).thenReturn(List.of(lowerCaseName));

            List<Hero> result = service.aggregateHeroData("14.10.1");

            assertEquals("A", result.get(0).getTier());
            assertEquals("high", result.get(0).getConfidenceLevel());
        }
    }

    @Nested
    @DisplayName("aggregateAugmentData 测试")
    class AggregateAugmentDataTest {

        @Test
        @DisplayName("正常聚合符文数据")
        void shouldAggregateAugmentData() {
            List<AramAugmentStatsDTO> stats = List.of(
                    buildAramAugmentStats("Small MR", "银色", 52.0, 15.0),
                    buildAramAugmentStats("Big CDR", "金色", 55.0, 8.0)
            );
            when(aramDataCollector.collectAugmentStats()).thenReturn(stats);

            List<Augment> result = service.aggregateAugmentData();

            assertEquals(2, result.size());
            assertEquals("Small MR", result.get(0).getNameEn());
            assertEquals("银色", result.get(0).getQuality());
            assertEquals(new BigDecimal("52.0"), result.get(0).getWinRate());
            assertEquals("S", result.get(0).getTier());
            assertEquals("精密", result.get(0).getSynergySet());
        }

        @Test
        @DisplayName("AramDataCollector 返回空列表")
        void shouldReturnEmptyWhenNoAugmentStats() {
            when(aramDataCollector.collectAugmentStats()).thenReturn(Collections.emptyList());

            List<Augment> result = service.aggregateAugmentData();

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("无效符文名称被跳过")
        void shouldSkipAugmentWithBlankName() {
            AramAugmentStatsDTO blankName = AramAugmentStatsDTO.builder()
                    .augmentName("")
                    .quality("银色")
                    .winRate(BigDecimal.valueOf(50.0))
                    .pickRate(BigDecimal.valueOf(10.0))
                    .avgPlacement(BigDecimal.valueOf(4.0))
                    .tier("A")
                    .build();
            AramAugmentStatsDTO validName = buildAramAugmentStats("Valid", "金色", 51.0, 12.0);
            when(aramDataCollector.collectAugmentStats()).thenReturn(List.of(blankName, validName));

            List<Augment> result = service.aggregateAugmentData();

            assertEquals(1, result.size());
            assertEquals("Valid", result.get(0).getNameEn());
        }

        @Test
        @DisplayName("胜率超出范围的符文被跳过")
        void shouldSkipAugmentWithOutOfRangeWinRate() {
            AramAugmentStatsDTO outOfRange = AramAugmentStatsDTO.builder()
                    .augmentName("Bad Augment")
                    .quality("银色")
                    .winRate(new BigDecimal("200.0"))
                    .pickRate(BigDecimal.valueOf(10.0))
                    .avgPlacement(BigDecimal.valueOf(4.0))
                    .tier("A")
                    .build();
            when(aramDataCollector.collectAugmentStats()).thenReturn(List.of(outOfRange));

            List<Augment> result = service.aggregateAugmentData();

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("英文品质名映射为中文")
        void shouldMapEnglishQualityToChinese() {
            AramAugmentStatsDTO englishQuality = AramAugmentStatsDTO.builder()
                    .augmentName("Test Augment")
                    .quality("prismatic")
                    .winRate(BigDecimal.valueOf(55.0))
                    .pickRate(BigDecimal.valueOf(10.0))
                    .avgPlacement(BigDecimal.valueOf(3.0))
                    .tier("S")
                    .synergySet("regeneration")
                    .build();
            when(aramDataCollector.collectAugmentStats()).thenReturn(List.of(englishQuality));

            List<Augment> result = service.aggregateAugmentData();

            assertEquals(1, result.size());
            assertEquals("棱彩", result.get(0).getQuality());
        }

        @Test
        @DisplayName("avgPlacement 超出范围被 clamp")
        void shouldClampOutOfRangeAvgPlacement() {
            AramAugmentStatsDTO outOfRange = AramAugmentStatsDTO.builder()
                    .augmentName("Test")
                    .quality("银色")
                    .winRate(BigDecimal.valueOf(50.0))
                    .pickRate(BigDecimal.valueOf(10.0))
                    .avgPlacement(new BigDecimal("15.0"))
                    .tier("B")
                    .build();
            when(aramDataCollector.collectAugmentStats()).thenReturn(List.of(outOfRange));

            List<Augment> result = service.aggregateAugmentData();

            assertEquals(1, result.size());
            assertEquals(new BigDecimal("8"), result.get(0).getAvgPlacement());
        }

        @Test
        @DisplayName("null 品质默认为银色")
        void shouldDefaultNullQualityToSilver() {
            AramAugmentStatsDTO nullQuality = AramAugmentStatsDTO.builder()
                    .augmentName("Test")
                    .quality(null)
                    .winRate(BigDecimal.valueOf(50.0))
                    .pickRate(BigDecimal.valueOf(10.0))
                    .avgPlacement(BigDecimal.valueOf(4.0))
                    .tier("A")
                    .build();
            when(aramDataCollector.collectAugmentStats()).thenReturn(List.of(nullQuality));

            List<Augment> result = service.aggregateAugmentData();

            assertEquals("银色", result.get(0).getQuality());
        }
    }

    @Nested
    @DisplayName("aggregateAndSaveHeroData 测试")
    class UpsertHeroDataTest {

        @Test
        @DisplayName("新英雄插入，已有英雄更新")
        void shouldInsertNewAndUpdateExisting() {
            ObjectNode aatrox = buildChampionJson("Aatrox", "亚托克斯", "暗裔剑魔");
            ObjectNode ahri = buildChampionJson("Ahri", "阿狸", "九尾妖狐");
            when(riotDataDragonClient.fetchChampionList("14.10.1"))
                    .thenReturn(Map.of("Aatrox", aatrox, "Ahri", ahri));
            when(aramDataCollector.collectAramStats()).thenReturn(Collections.emptyList());

            Hero existingAhri = new Hero();
            existingAhri.setId(1L);
            existingAhri.setNameEn("Ahri");
            when(heroMapper.selectOne(argThat(w -> w != null))).thenReturn(null, existingAhri);

            List<Hero> result = service.aggregateAndSaveHeroData("14.10.1");

            assertEquals(2, result.size());
            verify(heroMapper, times(1)).insert(any(Hero.class));
            verify(heroMapper, times(1)).updateById(any(Hero.class));
        }

        @Test
        @DisplayName("空聚合结果不执行数据库操作")
        void shouldSkipDatabaseWhenEmpty() {
            when(riotDataDragonClient.fetchChampionList("14.10.1")).thenReturn(Collections.emptyMap());

            List<Hero> result = service.aggregateAndSaveHeroData("14.10.1");

            assertTrue(result.isEmpty());
            verify(heroMapper, never()).insert(any(Hero.class));
            verify(heroMapper, never()).updateById(any(Hero.class));
        }
    }

    @Nested
    @DisplayName("aggregateAndSaveAugmentData 测试")
    class UpsertAugmentDataTest {

        @Test
        @DisplayName("新符文插入，已有符文更新")
        void shouldInsertNewAndUpdateExisting() {
            AramAugmentStatsDTO dto = AramAugmentStatsDTO.builder()
                    .augmentName("Small MR").quality("银色")
                    .winRate(BigDecimal.valueOf(52.0)).pickRate(BigDecimal.valueOf(15.0))
                    .avgPlacement(BigDecimal.valueOf(3.5)).tier("S").synergySet("精密")
                    .source("u.gg").build();
            when(aramDataCollector.collectAugmentStats()).thenReturn(List.of(dto));

            Augment existing = new Augment();
            existing.setId(5L);
            existing.setNameEn("Small MR");
            when(augmentMapper.selectOne(argThat(w -> w != null))).thenReturn(existing);

            List<Augment> result = service.aggregateAndSaveAugmentData();

            assertEquals(1, result.size());
            verify(augmentMapper, times(1)).updateById(any(Augment.class));
            verify(augmentMapper, never()).insert(any(Augment.class));
        }
    }
}
