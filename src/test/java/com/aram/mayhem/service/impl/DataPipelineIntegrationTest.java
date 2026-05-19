package com.aram.mayhem.service.impl;

import com.aram.mayhem.client.AramDataCollector;
import com.aram.mayhem.client.RiotDataDragonClient;
import com.aram.mayhem.dto.AramAugmentStatsDTO;
import com.aram.mayhem.dto.AramHeroStatsDTO;
import com.aram.mayhem.dto.ValidationResult;
import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.mapper.AugmentMapper;
import com.aram.mayhem.mapper.HeroMapper;
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
@DisplayName("数据管线集成测试：DataAggregator → MultiSourceValidator")
class DataPipelineIntegrationTest {

    @Mock
    private RiotDataDragonClient riotDataDragonClient;

    @Mock
    private AramDataCollector aramDataCollector;

    @Mock
    private HeroMapper heroMapper;

    @Mock
    private AugmentMapper augmentMapper;

    private DataAggregatorServiceImpl aggregatorService;
    private MultiSourceValidatorImpl validatorService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        aggregatorService = new DataAggregatorServiceImpl(riotDataDragonClient, aramDataCollector, heroMapper, augmentMapper);
        validatorService = new MultiSourceValidatorImpl();
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
        passive.put("name", "被动");
        passive.put("description", "被动描述");
        ArrayNode spells = node.putArray("spells");
        for (String key : new String[]{"Q", "W", "E", "R"}) {
            ObjectNode spell = spells.addObject();
            spell.put("name", key);
            spell.put("description", key + "描述");
        }
        return node;
    }

    @Nested
    @DisplayName("英雄数据管线：聚合 → 验证")
    class HeroPipelineTest {

        @Test
        @DisplayName("完整流程：双源数据聚合后验证通过")
        void shouldAggregateAndValidateHeroData() {
            ObjectNode aatrox = buildChampionJson("Aatrox", "亚托克斯", "暗裔剑魔");
            ObjectNode ahri = buildChampionJson("Ahri", "阿狸", "九尾妖狐");
            when(riotDataDragonClient.fetchChampionList("14.10.1"))
                    .thenReturn(Map.of("Aatrox", aatrox, "Ahri", ahri));

            List<AramHeroStatsDTO> aramStats = List.of(
                    AramHeroStatsDTO.builder().championName("Aatrox").tier("S")
                            .winRate(BigDecimal.valueOf(53.5)).pickRate(BigDecimal.valueOf(12.0))
                            .avgKills(BigDecimal.valueOf(5.0)).avgDeaths(BigDecimal.valueOf(4.0))
                            .avgAssists(BigDecimal.valueOf(8.0)).source("u.gg").build(),
                    AramHeroStatsDTO.builder().championName("Ahri").tier("A")
                            .winRate(BigDecimal.valueOf(51.2)).pickRate(BigDecimal.valueOf(8.5))
                            .avgKills(BigDecimal.valueOf(6.0)).avgDeaths(BigDecimal.valueOf(5.0))
                            .avgAssists(BigDecimal.valueOf(9.0)).source("u.gg").build()
            );
            when(aramDataCollector.collectAramStats()).thenReturn(aramStats);

            List<Hero> heroes = aggregatorService.aggregateHeroData("14.10.1");
            List<ValidationResult> results = validatorService.validateHeroStats(heroes);

            assertEquals(2, results.size());
            assertEquals(2, results.stream().filter(ValidationResult::isPassed).count());
            assertEquals(2, results.stream().filter(r -> "HIGH".equals(r.getConfidenceLevel())).count());
        }

        @Test
        @DisplayName("未匹配英雄聚合后验证为 LOW 置信度")
        void shouldMarkUnmatchedHeroAsLowConfidence() {
            ObjectNode aatrox = buildChampionJson("Aatrox", "亚托克斯", "暗裔剑魔");
            when(riotDataDragonClient.fetchChampionList("14.10.1"))
                    .thenReturn(Map.of("Aatrox", aatrox));
            when(aramDataCollector.collectAramStats()).thenReturn(Collections.emptyList());

            List<Hero> heroes = aggregatorService.aggregateHeroData("14.10.1");
            List<ValidationResult> results = validatorService.validateHeroStats(heroes);

            assertEquals(1, results.size());
            assertEquals("LOW", results.get(0).getConfidenceLevel());
            assertFalse(results.get(0).isPassed());
        }

        @Test
        @DisplayName("异常胜率数据聚合后验证为 LOW")
        void shouldMarkAbnormalWinRateAsLow() {
            ObjectNode aatrox = buildChampionJson("Aatrox", "亚托克斯", "暗裔剑魔");
            when(riotDataDragonClient.fetchChampionList("14.10.1"))
                    .thenReturn(Map.of("Aatrox", aatrox));

            AramHeroStatsDTO badStats = AramHeroStatsDTO.builder().championName("Aatrox").tier("S")
                    .winRate(new BigDecimal("150.0")).pickRate(BigDecimal.valueOf(12.0))
                    .source("u.gg").build();
            when(aramDataCollector.collectAramStats()).thenReturn(List.of(badStats));

            List<Hero> heroes = aggregatorService.aggregateHeroData("14.10.1");
            List<ValidationResult> results = validatorService.validateHeroStats(heroes);

            assertEquals("LOW", results.get(0).getConfidenceLevel());
        }
    }

    @Nested
    @DisplayName("符文数据管线：聚合 → 验证")
    class AugmentPipelineTest {

        @Test
        @DisplayName("完整流程：符文聚合后验证通过")
        void shouldAggregateAndValidateAugmentData() {
            List<AramAugmentStatsDTO> stats = List.of(
                    AramAugmentStatsDTO.builder().augmentName("Small MR").quality("银色")
                            .winRate(BigDecimal.valueOf(52.0)).pickRate(BigDecimal.valueOf(15.0))
                            .avgPlacement(BigDecimal.valueOf(3.5)).tier("S").synergySet("精密")
                            .source("u.gg").build(),
                    AramAugmentStatsDTO.builder().augmentName("Big CDR").quality("金色")
                            .winRate(BigDecimal.valueOf(55.0)).pickRate(BigDecimal.valueOf(8.0))
                            .avgPlacement(BigDecimal.valueOf(2.8)).tier("S+").synergySet("精密")
                            .source("u.gg").build()
            );
            when(aramDataCollector.collectAugmentStats()).thenReturn(stats);

            List<Augment> augments = aggregatorService.aggregateAugmentData();
            List<ValidationResult> results = validatorService.validateAugmentStats(augments);

            assertEquals(2, results.size());
            assertTrue(results.stream().allMatch(ValidationResult::isPassed));
        }

        @Test
        @DisplayName("无效符文被过滤后验证仅包含有效数据")
        void shouldFilterInvalidAugmentsBeforeValidation() {
            List<AramAugmentStatsDTO> stats = List.of(
                    AramAugmentStatsDTO.builder().augmentName("").quality("银色")
                            .winRate(BigDecimal.valueOf(50.0)).pickRate(BigDecimal.valueOf(10.0))
                            .avgPlacement(BigDecimal.valueOf(4.0)).tier("A").build(),
                    AramAugmentStatsDTO.builder().augmentName("Valid").quality("金色")
                            .winRate(BigDecimal.valueOf(52.0)).pickRate(BigDecimal.valueOf(12.0))
                            .avgPlacement(BigDecimal.valueOf(3.0)).tier("S").build()
            );
            when(aramDataCollector.collectAugmentStats()).thenReturn(stats);

            List<Augment> augments = aggregatorService.aggregateAugmentData();
            List<ValidationResult> results = validatorService.validateAugmentStats(augments);

            assertEquals(1, results.size());
            assertEquals("Valid", results.get(0).getTargetName());
        }
    }
}
