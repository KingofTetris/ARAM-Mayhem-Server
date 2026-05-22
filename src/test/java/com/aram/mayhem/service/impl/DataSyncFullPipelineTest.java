package com.aram.mayhem.service.impl;

import com.aram.mayhem.client.AramDataCollector;
import com.aram.mayhem.client.RiotDataDragonClient;
import com.aram.mayhem.dto.AramAugmentStatsDTO;
import com.aram.mayhem.dto.AramHeroStatsDTO;
import com.aram.mayhem.dto.SyncResult;
import com.aram.mayhem.dto.ValidationResult;
import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.mapper.AugmentMapper;
import com.aram.mayhem.mapper.HeroMapper;
import com.aram.mayhem.scheduler.DataSyncScheduler;
import com.aram.mayhem.service.CacheWarmupService;
import com.aram.mayhem.service.DataAggregatorService;
import com.aram.mayhem.service.DistributedLockService;
import com.aram.mayhem.service.MultiSourceValidator;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("M9 数据管线全链路集成测试")
class DataSyncFullPipelineTest {

    @Mock private RiotDataDragonClient riotDataDragonClient;
    @Mock private AramDataCollector aramDataCollector;
    @Mock private HeroMapper heroMapper;
    @Mock private AugmentMapper augmentMapper;
    @Mock private DistributedLockService distributedLockService;
    @Mock private CacheWarmupService cacheWarmupService;
    @Mock private MultiSourceValidator multiSourceValidator;
    @Mock private DataAggregatorService dataAggregatorService;
    @Mock private StringRedisTemplate stringRedisTemplate;

    private DataSyncScheduler scheduler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        scheduler = new DataSyncScheduler(
                dataAggregatorService,
                multiSourceValidator,
                distributedLockService,
                riotDataDragonClient,
                cacheWarmupService
        );
        ReflectionTestUtils.setField(scheduler, "warmupTopN", 50);
    }

    private Hero createHero(Long id, String nameEn, BigDecimal winRate, String tier) {
        Hero hero = new Hero();
        hero.setId(id);
        hero.setNameEn(nameEn);
        hero.setNameZh(nameEn);
        hero.setTitle("Test");
        hero.setRole("Fighter");
        hero.setTier(tier);
        hero.setWinRate(winRate);
        hero.setPickRate(new BigDecimal("10.00"));
        hero.setConfidenceLevel("high");
        return hero;
    }

    private Augment createAugment(Long id, String nameEn, BigDecimal winRate, String quality) {
        Augment augment = new Augment();
        augment.setId(id);
        augment.setNameEn(nameEn);
        augment.setNameZh(nameEn);
        augment.setQuality(quality);
        augment.setWinRate(winRate);
        augment.setPickRate(new BigDecimal("8.00"));
        augment.setTier("S");
        augment.setIsTrap(false);
        return augment;
    }

    @Nested
    @DisplayName("全链路：fetch → aggregate → validate → sync → warmup")
    class FullPipelineTest {

        @Test
        @DisplayName("完整同步流程成功：双源数据聚合+验证通过+缓存预热")
        void shouldCompleteFullSyncPipeline() {
            when(distributedLockService.tryLock(eq("data:sync"), anyLong())).thenReturn(true);
            when(riotDataDragonClient.fetchLatestVersion()).thenReturn("14.10.1");

            List<Hero> heroes = List.of(
                    createHero(1L, "Aatrox", new BigDecimal("53.50"), "S"),
                    createHero(2L, "Ahri", new BigDecimal("51.20"), "A")
            );
            List<Augment> augments = List.of(
                    createAugment(1L, "Eternal Winter", new BigDecimal("55.00"), "棱彩"),
                    createAugment(2L, "Small MR", new BigDecimal("52.00"), "银色")
            );

            when(dataAggregatorService.aggregateAndSaveHeroData("14.10.1")).thenReturn(heroes);
            when(dataAggregatorService.aggregateAndSaveAugmentData()).thenReturn(augments);

            ValidationResult heroVr1 = ValidationResult.builder().targetName("Aatrox").targetType("hero").confidenceLevel("HIGH").passed(true).build();
            ValidationResult heroVr2 = ValidationResult.builder().targetName("Ahri").targetType("hero").confidenceLevel("HIGH").passed(true).build();
            ValidationResult augVr1 = ValidationResult.builder().targetName("Eternal Winter").targetType("augment").confidenceLevel("HIGH").passed(true).build();
            ValidationResult augVr2 = ValidationResult.builder().targetName("Small MR").targetType("augment").confidenceLevel("HIGH").passed(true).build();

            when(multiSourceValidator.validateHeroStats(heroes)).thenReturn(List.of(heroVr1, heroVr2));
            when(multiSourceValidator.validateAugmentStats(augments)).thenReturn(List.of(augVr1, augVr2));

            when(cacheWarmupService.warmupHeroCache(50)).thenReturn(List.of(1L, 2L));
            when(cacheWarmupService.warmupAugmentCache()).thenReturn(2);
            when(cacheWarmupService.warmupHeroListCache()).thenReturn(2);
            when(cacheWarmupService.cleanupStaleCache()).thenReturn(0);

            SyncResult result = scheduler.syncAllData();

            assertTrue(result.isSuccess());
            assertEquals(4, result.getValidationPassed());
            assertEquals(0, result.getValidationFailed());
            assertTrue(result.getDurationMs() >= 0);

            verify(distributedLockService).tryLock(eq("data:sync"), anyLong());
            verify(dataAggregatorService).aggregateAndSaveHeroData("14.10.1");
            verify(dataAggregatorService).aggregateAndSaveAugmentData();
            verify(multiSourceValidator).validateHeroStats(heroes);
            verify(multiSourceValidator).validateAugmentStats(augments);
            verify(cacheWarmupService).warmupHeroCache(50);
            verify(cacheWarmupService).warmupAugmentCache();
            verify(cacheWarmupService).warmupHeroListCache();
            verify(cacheWarmupService).cleanupStaleCache();
            verify(distributedLockService).unlock("data:sync");
        }

        @Test
        @DisplayName("部分验证失败时同步仍成功但记录失败数")
        void shouldSucceedWithPartialValidationFailures() {
            when(distributedLockService.tryLock(eq("data:sync"), anyLong())).thenReturn(true);
            when(riotDataDragonClient.fetchLatestVersion()).thenReturn("14.10.1");

            List<Hero> heroes = List.of(
                    createHero(1L, "Aatrox", new BigDecimal("53.50"), "S"),
                    createHero(2L, "Ahri", new BigDecimal("150.00"), "S")
            );
            when(dataAggregatorService.aggregateAndSaveHeroData("14.10.1")).thenReturn(heroes);
            when(dataAggregatorService.aggregateAndSaveAugmentData()).thenReturn(List.of());

            ValidationResult vr1 = ValidationResult.builder().targetName("Aatrox").confidenceLevel("HIGH").passed(true).build();
            ValidationResult vr2 = ValidationResult.builder().targetName("Ahri").confidenceLevel("LOW").passed(false).build();
            when(multiSourceValidator.validateHeroStats(heroes)).thenReturn(List.of(vr1, vr2));
            when(multiSourceValidator.validateAugmentStats(List.of())).thenReturn(List.of());

            when(cacheWarmupService.warmupHeroCache(50)).thenReturn(List.of(1L));
            when(cacheWarmupService.warmupAugmentCache()).thenReturn(0);
            when(cacheWarmupService.warmupHeroListCache()).thenReturn(2);
            when(cacheWarmupService.cleanupStaleCache()).thenReturn(0);

            SyncResult result = scheduler.syncAllData();

            assertTrue(result.isSuccess());
            assertEquals(1, result.getValidationPassed());
            assertEquals(1, result.getValidationFailed());
        }

        @Test
        @DisplayName("分布式锁获取失败时跳过同步")
        void shouldSkipSyncWhenLockNotAcquired() {
            when(distributedLockService.tryLock(eq("data:sync"), anyLong())).thenReturn(false);

            SyncResult result = scheduler.syncAllData();

            assertFalse(result.isSuccess());
            assertNotNull(result.getErrorMessage());
            verify(dataAggregatorService, never()).aggregateAndSaveHeroData(anyString());
            verify(cacheWarmupService, never()).warmupHeroCache(anyInt());
        }

        @Test
        @DisplayName("DataDragon版本号获取失败时同步失败")
        void shouldFailWhenVersionFetchFails() {
            when(distributedLockService.tryLock(eq("data:sync"), anyLong())).thenReturn(true);
            when(riotDataDragonClient.fetchLatestVersion()).thenReturn(null);

            SyncResult result = scheduler.syncAllData();

            assertFalse(result.isSuccess());
            verify(dataAggregatorService, never()).aggregateAndSaveHeroData(anyString());
            verify(distributedLockService).unlock("data:sync");
        }

        @Test
        @DisplayName("DataDragon版本号返回空字符串时同步失败")
        void shouldFailWhenVersionIsEmpty() {
            when(distributedLockService.tryLock(eq("data:sync"), anyLong())).thenReturn(true);
            when(riotDataDragonClient.fetchLatestVersion()).thenReturn("  ");

            SyncResult result = scheduler.syncAllData();

            assertFalse(result.isSuccess());
            verify(dataAggregatorService, never()).aggregateAndSaveHeroData(anyString());
        }

        @Test
        @DisplayName("聚合异常时同步失败并释放锁")
        void shouldFailAndReleaseLockOnAggregationException() {
            when(distributedLockService.tryLock(eq("data:sync"), anyLong())).thenReturn(true);
            when(riotDataDragonClient.fetchLatestVersion()).thenReturn("14.10.1");
            when(dataAggregatorService.aggregateAndSaveHeroData("14.10.1"))
                    .thenThrow(new RuntimeException("DB connection lost"));

            SyncResult result = scheduler.syncAllData();

            assertFalse(result.isSuccess());
            verify(distributedLockService).unlock("data:sync");
            verify(cacheWarmupService, never()).warmupHeroCache(anyInt());
        }
    }

    @Nested
    @DisplayName("聚合→验证子链路")
    class AggregateValidatePipelineTest {

        private DataAggregatorServiceImpl aggregatorService;
        private MultiSourceValidatorImpl validatorService;

        @BeforeEach
        void initServices() {
            aggregatorService = new DataAggregatorServiceImpl(
                    riotDataDragonClient, aramDataCollector, heroMapper, augmentMapper);
            validatorService = new MultiSourceValidatorImpl();
        }

        @Test
        @DisplayName("英雄：双源匹配→HIGH置信度→验证通过")
        void shouldValidateMatchedHeroAsHigh() {
            ObjectNode aatrox = buildChampionJson("Aatrox", "亚托克斯", "暗裔剑魔");
            when(riotDataDragonClient.fetchChampionList("14.10.1"))
                    .thenReturn(Map.of("Aatrox", aatrox));

            AramHeroStatsDTO stats = AramHeroStatsDTO.builder()
                    .championName("Aatrox").tier("S")
                    .winRate(new BigDecimal("53.50")).pickRate(new BigDecimal("12.00"))
                    .avgKills(new BigDecimal("5.0")).avgDeaths(new BigDecimal("4.0"))
                    .avgAssists(new BigDecimal("8.0")).source("u.gg").build();
            when(aramDataCollector.collectAramStats()).thenReturn(List.of(stats));

            List<Hero> heroes = aggregatorService.aggregateHeroData("14.10.1");
            List<ValidationResult> results = validatorService.validateHeroStats(heroes);

            assertEquals(1, heroes.size());
            assertEquals("Aatrox", heroes.get(0).getNameEn());
            assertEquals(new BigDecimal("53.50"), heroes.get(0).getWinRate());
            assertEquals(1, results.size());
            assertTrue(results.get(0).isPassed());
            assertEquals("HIGH", results.get(0).getConfidenceLevel());
        }

        @Test
        @DisplayName("英雄：无ARAM数据→默认值→LOW置信度")
        void shouldMarkUnmatchedHeroAsLow() {
            ObjectNode aatrox = buildChampionJson("Aatrox", "亚托克斯", "暗裔剑魔");
            when(riotDataDragonClient.fetchChampionList("14.10.1"))
                    .thenReturn(Map.of("Aatrox", aatrox));
            when(aramDataCollector.collectAramStats()).thenReturn(List.of());

            List<Hero> heroes = aggregatorService.aggregateHeroData("14.10.1");
            List<ValidationResult> results = validatorService.validateHeroStats(heroes);

            assertEquals(new BigDecimal("50.00"), heroes.get(0).getWinRate());
            assertEquals("low", heroes.get(0).getConfidenceLevel());
            assertFalse(results.get(0).isPassed());
            assertEquals("LOW", results.get(0).getConfidenceLevel());
        }

        @Test
        @DisplayName("符文：有效数据→验证通过")
        void shouldValidateValidAugment() {
            AramAugmentStatsDTO dto = AramAugmentStatsDTO.builder()
                    .augmentName("Eternal Winter").quality("棱彩")
                    .winRate(new BigDecimal("55.00")).pickRate(new BigDecimal("8.00"))
                    .avgPlacement(new BigDecimal("3.50")).tier("S").synergySet("shield")
                    .source("u.gg").build();
            when(aramDataCollector.collectAugmentStats()).thenReturn(List.of(dto));

            List<Augment> augments = aggregatorService.aggregateAugmentData();
            List<ValidationResult> results = validatorService.validateAugmentStats(augments);

            assertEquals(1, augments.size());
            assertEquals("Eternal Winter", augments.get(0).getNameEn());
            assertTrue(results.get(0).isPassed());
        }

        @Test
        @DisplayName("符文：空名称被过滤")
        void shouldFilterAugmentWithBlankName() {
            AramAugmentStatsDTO invalid = AramAugmentStatsDTO.builder()
                    .augmentName("").quality("银色")
                    .winRate(new BigDecimal("50.00")).pickRate(new BigDecimal("10.00"))
                    .avgPlacement(new BigDecimal("4.00")).tier("A").build();
            AramAugmentStatsDTO valid = AramAugmentStatsDTO.builder()
                    .augmentName("Valid Augment").quality("金色")
                    .winRate(new BigDecimal("52.00")).pickRate(new BigDecimal("12.00"))
                    .avgPlacement(new BigDecimal("3.00")).tier("S").build();
            when(aramDataCollector.collectAugmentStats()).thenReturn(List.of(invalid, valid));

            List<Augment> augments = aggregatorService.aggregateAugmentData();

            assertEquals(1, augments.size());
            assertEquals("Valid Augment", augments.get(0).getNameEn());
        }

        @Test
        @DisplayName("数据清洗：胜率超出范围被clamp")
        void shouldClampOutOfRangeWinRate() {
            ObjectNode hero = buildChampionJson("Test", "测试", "测试英雄");
            when(riotDataDragonClient.fetchChampionList("14.10.1"))
                    .thenReturn(Map.of("Test", hero));

            AramHeroStatsDTO badStats = AramHeroStatsDTO.builder()
                    .championName("Test").tier("S")
                    .winRate(new BigDecimal("150.00")).pickRate(new BigDecimal("200.00"))
                    .source("u.gg").build();
            when(aramDataCollector.collectAramStats()).thenReturn(List.of(badStats));

            List<Hero> heroes = aggregatorService.aggregateHeroData("14.10.1");

            assertEquals(new BigDecimal("100"), heroes.get(0).getWinRate());
            assertEquals(new BigDecimal("100"), heroes.get(0).getPickRate());
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
    }

    @Nested
    @DisplayName("分布式锁竞争场景")
    class LockContentionTest {

        @Test
        @DisplayName("同步完成后锁被释放")
        void shouldReleaseLockAfterSuccessfulSync() {
            when(distributedLockService.tryLock(eq("data:sync"), anyLong())).thenReturn(true);
            when(riotDataDragonClient.fetchLatestVersion()).thenReturn("14.10.1");
            when(dataAggregatorService.aggregateAndSaveHeroData("14.10.1")).thenReturn(List.of());
            when(dataAggregatorService.aggregateAndSaveAugmentData()).thenReturn(List.of());
            when(multiSourceValidator.validateHeroStats(anyList())).thenReturn(List.of());
            when(multiSourceValidator.validateAugmentStats(anyList())).thenReturn(List.of());
            when(cacheWarmupService.warmupHeroCache(anyInt())).thenReturn(List.of());
            when(cacheWarmupService.warmupAugmentCache()).thenReturn(0);
            when(cacheWarmupService.warmupHeroListCache()).thenReturn(0);
            when(cacheWarmupService.cleanupStaleCache()).thenReturn(0);

            scheduler.syncAllData();

            verify(distributedLockService).unlock("data:sync");
        }

        @Test
        @DisplayName("异常时锁仍被释放（finally块）")
        void shouldReleaseLockEvenOnException() {
            when(distributedLockService.tryLock(eq("data:sync"), anyLong())).thenReturn(true);
            when(riotDataDragonClient.fetchLatestVersion()).thenReturn("14.10.1");
            when(dataAggregatorService.aggregateAndSaveHeroData("14.10.1"))
                    .thenThrow(new RuntimeException("Unexpected error"));

            scheduler.syncAllData();

            verify(distributedLockService).unlock("data:sync");
        }
    }
}
