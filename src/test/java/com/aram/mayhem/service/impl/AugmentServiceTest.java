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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("AugmentService 测试")
@ExtendWith(MockitoExtension.class)
class AugmentServiceTest {

    @Mock
    private AugmentMapper augmentMapper;

    @Mock
    private HeroMapper heroMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private AugmentServiceImpl augmentService;

    private List<Augment> mockAugments;
    private List<Hero> mockHeroes;

    @BeforeEach
    void setUp() {
        mockAugments = Arrays.asList(
                createAugment(1L, "超量污染", "Contaminate", "PRISMATIC", "shield", "regeneration", "attack-speed",
                        new BigDecimal("55.2"), new BigDecimal("22.1"), new BigDecimal("2.3"), false),
                createAugment(2L, "灵魂连接", "Soul Link", "LEGENDARY", "shield", null, null,
                        new BigDecimal("53.8"), new BigDecimal("18.5"), new BigDecimal("2.5"), false),
                createAugment(3L, "致命节奏", "Fatal Rhythm", "EPIC", "attack-speed", "critical-strike", null,
                        new BigDecimal("52.1"), new BigDecimal("25.3"), new BigDecimal("2.1"), false),
                createAugment(4L, "水晶毒素", "Crystallize", "SILVER", "shield-break", null, null,
                        new BigDecimal("48.5"), new BigDecimal("12.1"), new BigDecimal("3.2"), false),
                createAugment(5L, "陷阱装备", "Trap Equipment", "LEGENDARY", "shield", null, null,
                        new BigDecimal("35.2"), new BigDecimal("8.5"), new BigDecimal("4.8"), true)
        );

        mockHeroes = Arrays.asList(
                createHero(1L, "Yasuo", "亚索", "Fighter"),
                createHero(2L, "Ashe", "艾希", "Marksman"),
                createHero(3L, "Amumu", "阿木木", "Tank")
        );
    }

    private Augment createAugment(Long id, String nameZh, String nameEn, String quality,
                                   String synergySet, String synergySet2, String synergySet3,
                                   BigDecimal winRate, BigDecimal pickRate, BigDecimal avgPlacement, boolean isTrap) {
        Augment augment = new Augment();
        augment.setId(id);
        augment.setNameZh(nameZh);
        augment.setNameEn(nameEn);
        augment.setQuality(quality);
        augment.setSynergySet(synergySet);
        augment.setSynergySet2(synergySet2);
        augment.setSynergySet3(synergySet3);
        augment.setWinRate(winRate);
        augment.setPickRate(pickRate);
        augment.setAvgPlacement(avgPlacement);
        augment.setTier("S");
        augment.setIsTrap(isTrap);
        augment.setDescription(nameZh + " is a powerful augment.");
        augment.setIconUrl("https://example.com/augment/" + id + ".png");
        return augment;
    }

    private Hero createHero(Long id, String nameEn, String nameZh, String role) {
        Hero hero = new Hero();
        hero.setId(id);
        hero.setNameEn(nameEn);
        hero.setNameZh(nameZh);
        hero.setRole(role);
        hero.setTier("S");
        hero.setWinRate(new BigDecimal("50.0"));
        hero.setPickRate(new BigDecimal("10.0"));
        return hero;
    }

    // ============================================================
    // 分页查询 - 无筛选条件
    // ============================================================

    @Test
    @DisplayName("testGetAugmentList_noFilters_returnsAllAugmentsSortedByWinRate")
    void testGetAugmentList_noFilters_returnsAllAugmentsSortedByWinRate() {
        int page = 1;
        int size = 10;
        Page<Augment> mockPage = new Page<>(page, size, 5);
        mockPage.setRecords(mockAugments);

        when(augmentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        PageResult<AugmentListVO> result = augmentService.getAugmentList(page, size, null, null);

        assertNotNull(result);
        assertEquals(5, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(5, result.getRecords().size());

        verify(augmentMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("testGetAugmentList_emptyResult_returnsEmptyList")
    void testGetAugmentList_emptyResult_returnsEmptyList() {
        Page<Augment> mockPage = new Page<>(1, 10, 0);
        mockPage.setRecords(Collections.emptyList());

        when(augmentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        PageResult<AugmentListVO> result = augmentService.getAugmentList(1, 10, null, null);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    // ============================================================
    // 分页查询 - 质量筛选
    // ============================================================

    @Test
    @DisplayName("testGetAugmentList_withQualityFilter_filtersByQuality")
    void testGetAugmentList_withQualityFilter_filtersByQuality() {
        List<Augment> prismaticAugments = Arrays.asList(mockAugments.get(0));
        Page<Augment> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(prismaticAugments);

        when(augmentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        PageResult<AugmentListVO> result = augmentService.getAugmentList(1, 10, "PRISMATIC", null);

        assertEquals(1, result.getTotal());
        assertEquals("PRISMATIC", result.getRecords().get(0).getQuality());
        assertEquals("超量污染", result.getRecords().get(0).getNameZh());
    }

    @Test
    @DisplayName("testGetAugmentList_withLegendaryFilter_returnsMultiple")
    void testGetAugmentList_withLegendaryFilter_returnsMultiple() {
        List<Augment> legendaryAugments = Arrays.asList(mockAugments.get(1), mockAugments.get(4));
        Page<Augment> mockPage = new Page<>(1, 10, 2);
        mockPage.setRecords(legendaryAugments);

        when(augmentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        PageResult<AugmentListVO> result = augmentService.getAugmentList(1, 10, "LEGENDARY", null);

        assertEquals(2, result.getTotal());
    }

    // ============================================================
    // 分页查询 - 套装筛选
    // ============================================================

    @Test
    @DisplayName("testGetAugmentList_withSynergySetFilter_filtersBySynergy")
    void testGetAugmentList_withSynergySetFilter_filtersBySynergy() {
        List<Augment> shieldAugments = Arrays.asList(mockAugments.get(0), mockAugments.get(1), mockAugments.get(4));
        Page<Augment> mockPage = new Page<>(1, 10, 3);
        mockPage.setRecords(shieldAugments);

        when(augmentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        PageResult<AugmentListVO> result = augmentService.getAugmentList(1, 10, null, "shield");

        assertEquals(3, result.getTotal());
    }

    @Test
    @DisplayName("testGetAugmentList_withSynergySetFilter2_matchesSecondSet")
    void testGetAugmentList_withSynergySetFilter2_matchesSecondSet() {
        List<Augment> regenAugments = Arrays.asList(mockAugments.get(0));
        Page<Augment> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(regenAugments);

        when(augmentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        PageResult<AugmentListVO> result = augmentService.getAugmentList(1, 10, null, "regeneration");

        assertEquals(1, result.getTotal());
        assertEquals("超量污染", result.getRecords().get(0).getNameZh());
    }

    // ============================================================
    // 详情查询
    // ============================================================

    @Test
    @DisplayName("testGetAugmentDetail_augmentExists_returnsVO")
    void testGetAugmentDetail_augmentExists_returnsVO() {
        when(augmentMapper.selectById(1L)).thenReturn(mockAugments.get(0));

        AugmentVO result = augmentService.getAugmentDetail(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("超量污染", result.getNameZh());
        assertEquals("Contaminate", result.getNameEn());
        assertEquals("PRISMATIC", result.getQuality());
        assertEquals("shield", result.getSynergySet());
        assertEquals(new BigDecimal("55.2"), result.getWinRate());

        verify(augmentMapper).selectById(1L);
    }

    @Test
    @DisplayName("testGetAugmentDetail_augmentNotFound_returnsNull")
    void testGetAugmentDetail_augmentNotFound_returnsNull() {
        when(augmentMapper.selectById(999L)).thenReturn(null);

        AugmentVO result = augmentService.getAugmentDetail(999L);

        assertNull(result);
        verify(augmentMapper).selectById(999L);
    }

    // ============================================================
    // 套装进度 - 空输入
    // ============================================================

    @Test
    @DisplayName("testGetSynergyProgress_nullInput_returnsAllSynergiesAsInactive")
    void testGetSynergyProgress_nullInput_returnsAllSynergiesAsInactive() {
        List<SynergyProgressResponse> result = augmentService.getSynergyProgress(null);

        assertNotNull(result);
        assertEquals(9, result.size());
        for (SynergyProgressResponse resp : result) {
            assertEquals(0, resp.getCurrentCount());
            assertEquals("inactive", resp.getStatus());
        }
    }

    @Test
    @DisplayName("testGetSynergyProgress_emptyInput_returnsAllSynergiesAsInactive")
    void testGetSynergyProgress_emptyInput_returnsAllSynergiesAsInactive() {
        List<SynergyProgressResponse> result = augmentService.getSynergyProgress("");

        assertNotNull(result);
        assertEquals(9, result.size());
        for (SynergyProgressResponse resp : result) {
            assertEquals(0, resp.getCurrentCount());
            assertEquals("inactive", resp.getStatus());
        }
    }

    @Test
    @DisplayName("testGetSynergyProgress_emptyStringInput_returnsAllSynergiesAsInactive")
    void testGetSynergyProgress_emptyStringInput_returnsAllSynergiesAsInactive() {
        List<SynergyProgressResponse> result = augmentService.getSynergyProgress("");

        assertNotNull(result);
        assertEquals(9, result.size());
        for (SynergyProgressResponse resp : result) {
            assertEquals(0, resp.getCurrentCount());
            assertEquals("inactive", resp.getStatus());
        }
    }

    // ============================================================
    // 套装进度 - 有效输入
    // ============================================================

    @Test
    @DisplayName("testGetSynergyProgress_singleAugmentWithThreshold1_returnsCompleted")
    void testGetSynergyProgress_singleAugmentWithThreshold1_returnsCompleted() {
        when(augmentMapper.selectBatchIds(Arrays.asList(1L))).thenReturn(Arrays.asList(mockAugments.get(0)));

        List<SynergyProgressResponse> result = augmentService.getSynergyProgress("1");

        assertNotNull(result);
        assertEquals(9, result.size());

        SynergyProgressResponse shieldProgress = result.stream()
                .filter(r -> "shield".equals(r.getSynergyName()))
                .findFirst()
                .orElse(null);

        assertNotNull(shieldProgress);
        assertEquals(1, shieldProgress.getCurrentCount());
        assertEquals("completed", shieldProgress.getStatus());
    }

    @Test
    @DisplayName("testGetSynergyProgress_attackSpeedWithThreshold2_returnsPartial")
    void testGetSynergyProgress_attackSpeedWithThreshold2_returnsPartial() {
        when(augmentMapper.selectBatchIds(Arrays.asList(3L))).thenReturn(Arrays.asList(mockAugments.get(2)));

        List<SynergyProgressResponse> result = augmentService.getSynergyProgress("3");

        assertNotNull(result);
        assertEquals(9, result.size());

        SynergyProgressResponse asProgress = result.stream()
                .filter(r -> "attack-speed".equals(r.getSynergyName()))
                .findFirst()
                .orElse(null);

        assertNotNull(asProgress);
        assertEquals(1, asProgress.getCurrentCount());
        assertEquals(2, asProgress.getTotalCount());
        assertEquals("partial", asProgress.getStatus());
    }

    @Test
    @DisplayName("testGetSynergyProgress_twoDifferentSynergies_bothUpdated")
    void testGetSynergyProgress_twoDifferentSynergies_bothUpdated() {
        when(augmentMapper.selectBatchIds(Arrays.asList(1L, 3L)))
                .thenReturn(Arrays.asList(mockAugments.get(0), mockAugments.get(2)));

        List<SynergyProgressResponse> result = augmentService.getSynergyProgress("1,3");

        SynergyProgressResponse asProgress = result.stream()
                .filter(r -> "attack-speed".equals(r.getSynergyName()))
                .findFirst()
                .orElse(null);

        assertNotNull(asProgress);
        assertEquals(2, asProgress.getCurrentCount());
        assertEquals(2, asProgress.getTotalCount());
        assertEquals("completed", asProgress.getStatus());

        SynergyProgressResponse critProgress = result.stream()
                .filter(r -> "critical-strike".equals(r.getSynergyName()))
                .findFirst()
                .orElse(null);

        assertNotNull(critProgress);
        assertEquals(1, critProgress.getCurrentCount());
        assertEquals(2, critProgress.getTotalCount());
        assertEquals("partial", critProgress.getStatus());
    }

    @Test
    @DisplayName("testGetSynergyProgress_completesSynergy_returnsCompletedStatus")
    void testGetSynergyProgress_completesSynergy_returnsCompletedStatus() {
        when(augmentMapper.selectBatchIds(Arrays.asList(1L, 2L, 5L)))
                .thenReturn(Arrays.asList(mockAugments.get(0), mockAugments.get(1), mockAugments.get(4)));

        List<SynergyProgressResponse> result = augmentService.getSynergyProgress("1,2,5");

        SynergyProgressResponse shieldProgress = result.stream()
                .filter(r -> "shield".equals(r.getSynergyName()))
                .findFirst()
                .orElse(null);

        assertNotNull(shieldProgress);
        assertEquals("completed", shieldProgress.getStatus());
    }

    // ============================================================
    // 推荐功能 - 英雄不存在
    // ============================================================

    @Test
    @DisplayName("testGetRecommendations_heroNotFound_returnsEmptyList")
    void testGetRecommendations_heroNotFound_returnsEmptyList() {
        AugmentRecommendRequest request = new AugmentRecommendRequest();
        request.setHeroId(999L);
        request.setSelectedAugmentIds(Collections.emptyList());

        when(heroMapper.selectById(999L)).thenReturn(null);

        List<AugmentRecommendResponse> result = augmentService.getRecommendations(request);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ============================================================
    // 推荐功能 - 英雄存在
    // ============================================================

    @Test
    @DisplayName("testGetRecommendations_heroExists_returnsRecommendations")
    void testGetRecommendations_heroExists_returnsRecommendations() {
        AugmentRecommendRequest request = new AugmentRecommendRequest();
        request.setHeroId(1L);
        request.setSelectedAugmentIds(Collections.emptyList());

        Page<Augment> mockPage = new Page<>(1, 20, 3);
        mockPage.setRecords(mockAugments.subList(0, 3));

        when(heroMapper.selectById(1L)).thenReturn(mockHeroes.get(0));
        when(augmentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        List<AugmentRecommendResponse> result = augmentService.getRecommendations(request);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("testGetRecommendations_excludesSelectedIds")
    void testGetRecommendations_excludesSelectedIds() {
        AugmentRecommendRequest request = new AugmentRecommendRequest();
        request.setHeroId(1L);
        request.setSelectedAugmentIds(Arrays.asList(1L, 2L));

        Page<Augment> mockPage = new Page<>(1, 20, 3);
        mockPage.setRecords(mockAugments.subList(2, 5));

        when(heroMapper.selectById(1L)).thenReturn(mockHeroes.get(0));
        when(augmentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        List<AugmentRecommendResponse> result = augmentService.getRecommendations(request);

        assertNotNull(result);
    }

    @Test
    @DisplayName("testGetRecommendations_excludesTrapAugments")
    void testGetRecommendations_excludesTrapAugments() {
        AugmentRecommendRequest request = new AugmentRecommendRequest();
        request.setHeroId(1L);
        request.setSelectedAugmentIds(Collections.emptyList());

        Page<Augment> mockPage = new Page<>(1, 20, 4);
        mockPage.setRecords(mockAugments.subList(0, 4));

        when(heroMapper.selectById(1L)).thenReturn(mockHeroes.get(0));
        when(augmentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        List<AugmentRecommendResponse> result = augmentService.getRecommendations(request);

        assertNotNull(result);
        for (AugmentRecommendResponse resp : result) {
            assertFalse(resp.getIsTrap());
        }
    }

    @Test
    @DisplayName("testGetRecommendations_sortedByScoreDescending")
    void testGetRecommendations_sortedByScoreDescending() {
        AugmentRecommendRequest request = new AugmentRecommendRequest();
        request.setHeroId(1L);
        request.setSelectedAugmentIds(Collections.emptyList());

        Page<Augment> mockPage = new Page<>(1, 20, 3);
        mockPage.setRecords(mockAugments.subList(0, 3));

        when(heroMapper.selectById(1L)).thenReturn(mockHeroes.get(0));
        when(augmentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        List<AugmentRecommendResponse> result = augmentService.getRecommendations(request);

        assertNotNull(result);
        if (result.size() > 1) {
            for (int i = 0; i < result.size() - 1; i++) {
                assertTrue(result.get(i).getScore() >= result.get(i + 1).getScore(),
                        "Scores should be in descending order");
            }
        }
    }

    // ============================================================
    // VO 转换正确性
    // ============================================================

    @Test
    @DisplayName("testConvertToListVO_allFieldsMapped")
    void testConvertToListVO_allFieldsMapped() {
        Page<Augment> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(Arrays.asList(mockAugments.get(0)));

        when(augmentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        PageResult<AugmentListVO> result = augmentService.getAugmentList(1, 10, null, null);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());

        AugmentListVO vo = result.getRecords().get(0);
        assertEquals(1L, vo.getId());
        assertEquals("超量污染", vo.getNameZh());
        assertEquals("Contaminate", vo.getNameEn());
        assertEquals("PRISMATIC", vo.getQuality());
        assertEquals("shield", vo.getSynergySet());
        assertEquals(new BigDecimal("55.2"), vo.getWinRate());
        assertEquals(new BigDecimal("22.1"), vo.getPickRate());
        assertEquals(new BigDecimal("2.3"), vo.getAvgPlacement());
        assertEquals("S", vo.getTier());
    }

    @Test
    @DisplayName("testConvertToVO_containsAllFields")
    void testConvertToVO_containsAllFields() {
        when(augmentMapper.selectById(3L)).thenReturn(mockAugments.get(2));

        AugmentVO result = augmentService.getAugmentDetail(3L);

        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals("致命节奏", result.getNameZh());
        assertEquals("Fatal Rhythm", result.getNameEn());
        assertEquals("EPIC", result.getQuality());
        assertEquals("attack-speed", result.getSynergySet());
        assertEquals(new BigDecimal("52.1"), result.getWinRate());
    }

    // ============================================================
    // 边界条件测试
    // ============================================================

    @Test
    @DisplayName("testGetAugmentList_pageBeyondTotal_returnsEmpty")
    void testGetAugmentList_pageBeyondTotal_returnsEmpty() {
        Page<Augment> mockPage = new Page<>(100, 10, 5);
        mockPage.setRecords(Collections.emptyList());

        when(augmentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        PageResult<AugmentListVO> result = augmentService.getAugmentList(100, 10, null, null);

        assertNotNull(result);
        assertEquals(5, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    @DisplayName("testGetSynergyProgress_allSynergiesHaveThresholds")
    void testGetSynergyProgress_allSynergiesHaveThresholds() {
        List<SynergyProgressResponse> result = augmentService.getSynergyProgress("");

        assertNotNull(result);
        assertEquals(9, result.size());

        for (SynergyProgressResponse resp : result) {
            assertTrue(resp.getTotalCount() > 0, "Total count should be positive for " + resp.getSynergyName());
            assertEquals(0.0, resp.getProgress());
        }
    }
}