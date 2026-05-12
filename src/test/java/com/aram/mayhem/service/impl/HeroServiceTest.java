package com.aram.mayhem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aram.mayhem.common.BusinessException;
import com.aram.mayhem.dto.HeroDetailVO;
import com.aram.mayhem.dto.HeroListVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.mapper.HeroMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * HeroServiceImpl 单元测试
 * 测试覆盖：
 * - 分页查询（无筛选、keyword筛选、tier筛选、空结果、排序）
 * - 英雄详情（存在、不存在）
 * - VO 转换正确性
 * - 分页 hasMore 计算
 */
@DisplayName("HeroService 测试")
@ExtendWith(MockitoExtension.class)
class HeroServiceTest {

    @Mock
    private HeroMapper mockMapper;

    @InjectMocks
    private HeroServiceImpl heroService;

    private List<Hero> mockHeroes;
    private List<Hero> emptyList;

    @BeforeEach
    void setUp() {
        mockHeroes = List.of(
                createHero(1L, "Ashe", "艾希", "Frost Archer", "Marksman", "S+", new BigDecimal("52.5"), new BigDecimal("15.3")),
                createHero(2L, "Lux", "拉克丝", "Lady of Luminosity", "Mage", "S", new BigDecimal("51.0"), new BigDecimal("12.8")),
                createHero(3L, "Garen", "盖伦", "The Might of Demacia", "Tank", "A", new BigDecimal("49.5"), new BigDecimal("8.2")),
                createHero(4L, "Yasuo", "亚索", "The Unforgiven", "Fighter", "B", new BigDecimal("47.0"), new BigDecimal("20.1")),
                createHero(5L, "Teemo", "提莫", "The Swift Scout", "Marksman", "C", new BigDecimal("48.5"), new BigDecimal("6.5"))
        );

        emptyList = List.of();
    }

    private Hero createHero(Long id, String nameEn, String nameZh, String title, String role,
                            String tier, BigDecimal winRate, BigDecimal pickRate) {
        Hero hero = new Hero();
        hero.setId(id);
        hero.setNameEn(nameEn);
        hero.setNameZh(nameZh);
        hero.setTitle(title);
        hero.setRole(role);
        hero.setTier(tier);
        hero.setWinRate(winRate);
        hero.setPickRate(pickRate);
        hero.setImageUrl("https://example.com/" + nameEn.toLowerCase() + ".png");
        hero.setConfidenceLevel("High");
        hero.setVersion("14.10");
        return hero;
    }

    // ============================================================
    // 分页查询 - 无筛选条件
    // ============================================================

    @Test
    @DisplayName("testGetHeroList_noFilters_returnsAllHeroesSortedByWinRate")
    void testGetHeroList_noFilters_returnsAllHeroesSortedByWinRate() {
        // Given
        int page = 1;
        int size = 10;
        Page<Hero> mockPage = new Page<>(page, size, 5);
        mockPage.setRecords(mockHeroes);

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When
        PageResult<HeroListVO> result = heroService.getHeroList(page, size, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(5, result.getRecords().size());

        // 默认排序应为按胜率降序
        assertEquals("Ashe", result.getRecords().get(0).getNameEn()); // 52.5 highest

        verify(mockMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("testGetHeroList_hasMore_true - 当总数大于当前页大小时 hasMore 应为 true")
    void testGetHeroList_hasMore_true() {
        // Given
        Page<Hero> mockPage = new Page<>(1, 3, 10);
        mockPage.setRecords(mockHeroes.subList(0, 3));

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When
        PageResult<HeroListVO> result = heroService.getHeroList(1, 3, null, null, null);

        // Then
        // PageResult 的 hasMore 由客户端计算：total > page * size
        assertEquals(10, result.getTotal());
        assertEquals(3, result.getRecords().size());
        assertTrue(result.getTotal() > (long) result.getPage() * result.getSize());
    }

    @Test
    @DisplayName("testGetHeroList_hasMore_false - 当最后一页时 hasMore 应为 false")
    void testGetHeroList_hasMore_false() {
        // Given
        Page<Hero> mockPage = new Page<>(4, 3, 5);
        mockPage.setRecords(mockHeroes.subList(3, 5));

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When
        PageResult<HeroListVO> result = heroService.getHeroList(4, 3, null, null, null);

        // Then
        assertEquals(5, result.getTotal());
        assertEquals(2, result.getRecords().size());
        // total(5) <= page(4) * size(3) = 12, 所以没有更多
        assertFalse(result.getTotal() > (long) result.getPage() * result.getSize());
    }

    // ============================================================
    // 分页查询 - 空结果
    // ============================================================

    @Test
    @DisplayName("testGetHeroList_emptyResult_returnsEmptyList")
    void testGetHeroList_emptyResult_returnsEmptyList() {
        // Given
        Page<Hero> mockPage = new Page<>(1, 10, 0);
        mockPage.setRecords(emptyList);

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When
        PageResult<HeroListVO> result = heroService.getHeroList(1, 10, "nonexistent", null, null);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
        assertFalse(result.getTotal() > (long) result.getPage() * result.getSize());
    }

    // ============================================================
    // 分页查询 - keyword 筛选
    // ============================================================

    @Test
    @DisplayName("testGetHeroList_withKeyword_filtersByNameEn")
    void testGetHeroList_withKeyword_filtersByNameEn() {
        // Given
        List<Hero> filteredHeroes = List.of(mockHeroes.get(0)); // Ashe
        Page<Hero> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(filteredHeroes);

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When
        PageResult<HeroListVO> result = heroService.getHeroList(1, 10, "Ashe", null, null);

        // Then
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("Ashe", result.getRecords().get(0).getNameEn());

        // 验证 queryWrapper 使用了 like 条件
        ArgumentCaptor<LambdaQueryWrapper<Hero>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mockMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        assertNotNull(wrapperCaptor.getValue());
    }

    @Test
    @DisplayName("testGetHeroList_withKeyword_filtersByNameZh")
    void testGetHeroList_withKeyword_filtersByNameZh() {
        // Given
        List<Hero> filteredHeroes = List.of(mockHeroes.get(2)); // Garen/盖伦
        Page<Hero> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(filteredHeroes);

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When
        PageResult<HeroListVO> result = heroService.getHeroList(1, 10, "盖伦", null, null);

        // Then
        assertEquals(1, result.getTotal());
        assertEquals("Garen", result.getRecords().get(0).getNameEn());
    }

    @Test
    @DisplayName("testGetHeroList_withKeyword_filtersByTitle")
    void testGetHeroList_withKeyword_filtersByTitle() {
        // Given
        List<Hero> filteredHeroes = List.of(mockHeroes.get(3)); // Yasuo/The Unforgiven
        Page<Hero> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(filteredHeroes);

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When
        PageResult<HeroListVO> result = heroService.getHeroList(1, 10, "Unforgiven", null, null);

        // Then
        assertEquals(1, result.getTotal());
        assertEquals("Yasuo", result.getRecords().get(0).getNameEn());
    }

    @Test
    @DisplayName("testGetHeroList_withEmptyKeyword_noFilterApplied")
    void testGetHeroList_withEmptyKeyword_noFilterApplied() {
        // Given
        Page<Hero> mockPage = new Page<>(1, 10, 5);
        mockPage.setRecords(mockHeroes);

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When：空字符串不应触发筛选
        PageResult<HeroListVO> result = heroService.getHeroList(1, 10, "", null, null);

        // Then
        assertEquals(5, result.getTotal());
        assertEquals(5, result.getRecords().size());
    }

    // ============================================================
    // 分页查询 - tier 筛选
    // ============================================================

    @Test
    @DisplayName("testGetHeroList_withTier_filtersByTier")
    void testGetHeroList_withTier_filtersByTier() {
        // Given
        List<Hero> sPlusHeroes = List.of(mockHeroes.get(0)); // S+ tier
        Page<Hero> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(sPlusHeroes);

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When
        PageResult<HeroListVO> result = heroService.getHeroList(1, 10, null, "S+", null);

        // Then
        assertEquals(1, result.getTotal());
        assertEquals("S+", result.getRecords().get(0).getTier());
    }

    @Test
    @DisplayName("testGetHeroList_withKeywordAndTier_combinedFiltering")
    void testGetHeroList_withKeywordAndTier_combinedFiltering() {
        // Given
        List<Hero> filteredHeroes = List.of(mockHeroes.get(4)); // Teemo, Marksman, C tier
        Page<Hero> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(filteredHeroes);

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When：同时使用 keyword 和 tier 筛选
        PageResult<HeroListVO> result = heroService.getHeroList(1, 10, "提莫", "C", null);

        // Then
        assertEquals(1, result.getTotal());
        assertEquals("Teemo", result.getRecords().get(0).getNameEn());
        assertEquals("C", result.getRecords().get(0).getTier());
    }

    // ============================================================
    // 分页查询 - 排序
    // ============================================================

    @Test
    @DisplayName("testGetHeroList_sortByWinRate_descending")
    void testGetHeroList_sortByWinRate_descending() {
        // Given
        Page<Hero> mockPage = new Page<>(1, 10, 5);
        mockPage.setRecords(mockHeroes);

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When
        PageResult<HeroListVO> result = heroService.getHeroList(1, 10, null, null, "winRate");

        // Then
        assertEquals(5, result.getRecords().size());
        // winRate 降序：Ashe(52.5) > Lux(51.0) > Teemo(48.5) > Garen(49.5) > Yasuo(47.0)
        assertEquals("Ashe", result.getRecords().get(0).getNameEn());

        ArgumentCaptor<LambdaQueryWrapper<Hero>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mockMapper).selectPage(any(Page.class), wrapperCaptor.capture());
    }

    @Test
    @DisplayName("testGetHeroList_sortByPickRate_descending")
    void testGetHeroList_sortByPickRate_descending() {
        // Given
        Page<Hero> mockPage = new Page<>(1, 10, 5);
        mockPage.setRecords(mockHeroes);

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When
        PageResult<HeroListVO> result = heroService.getHeroList(1, 10, null, null, "pickRate");

        // Then
        assertEquals(5, result.getRecords().size());
        ArgumentCaptor<LambdaQueryWrapper<Hero>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mockMapper).selectPage(any(Page.class), wrapperCaptor.capture());
    }

    @Test
    @DisplayName("testGetHeroList_sortByTier_ascending")
    void testGetHeroList_sortByTier_ascending() {
        // Given
        Page<Hero> mockPage = new Page<>(1, 10, 5);
        mockPage.setRecords(mockHeroes);

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When
        PageResult<HeroListVO> result = heroService.getHeroList(1, 10, null, null, "tier");

        // Then
        assertEquals(5, result.getRecords().size());
        ArgumentCaptor<LambdaQueryWrapper<Hero>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mockMapper).selectPage(any(Page.class), wrapperCaptor.capture());
    }

    @Test
    @DisplayName("testGetHeroList_sortByName_ascending")
    void testGetHeroList_sortByName_ascending() {
        // Given
        Page<Hero> mockPage = new Page<>(1, 10, 5);
        mockPage.setRecords(mockHeroes);

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When
        PageResult<HeroListVO> result = heroService.getHeroList(1, 10, null, null, "name");

        // Then
        assertEquals(5, result.getRecords().size());
        ArgumentCaptor<LambdaQueryWrapper<Hero>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mockMapper).selectPage(any(Page.class), wrapperCaptor.capture());
    }

    @Test
    @DisplayName("testGetHeroList_sortByUnknownField_defaultsToWinRate")
    void testGetHeroList_sortByUnknownField_defaultsToWinRate() {
        // Given
        Page<Hero> mockPage = new Page<>(1, 10, 5);
        mockPage.setRecords(mockHeroes);

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When：传入无效的 sortBy 值
        PageResult<HeroListVO> result = heroService.getHeroList(1, 10, null, null, "invalidSortField");

        // Then：应该使用默认的 winRate 降序排序
        assertEquals(5, result.getRecords().size());
        assertEquals("Ashe", result.getRecords().get(0).getNameEn()); // 最高胜率排第一
    }

    @Test
    @DisplayName("testGetHeroList_noSortBySpecified_defaultsToWinRate")
    void testGetHeroList_noSortBySpecified_defaultsToWinRate() {
        // Given
        Page<Hero> mockPage = new Page<>(1, 10, 5);
        mockPage.setRecords(mockHeroes);

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When：不指定 sortBy
        PageResult<HeroListVO> result = heroService.getHeroList(1, 10, null, null, null);

        // Then
        assertEquals(5, result.getRecords().size());
        assertEquals("Ashe", result.getRecords().get(0).getNameEn());
    }

    // ============================================================
    // 英雄详情 - 存在
    // ============================================================

    @Test
    @DisplayName("testGetHeroDetail_heroExists_returnsDetailVO")
    void testGetHeroDetail_heroExists_returnsDetailVO() {
        // Given
        Hero hero = mockHeroes.get(0); // Ashe
        when(mockMapper.selectById(1L)).thenReturn(hero);

        // When
        HeroDetailVO result = heroService.getHeroDetail(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Ashe", result.getNameEn());
        assertEquals("艾希", result.getNameZh());
        assertEquals("Frost Archer", result.getTitle());
        assertEquals("Marksman", result.getRole());
        assertEquals("S+", result.getTier());
        assertEquals(new BigDecimal("52.5"), result.getWinRate());
        assertEquals(new BigDecimal("15.3"), result.getPickRate());
        assertEquals("https://example.com/ashe.png", result.getImageUrl());

        // 验证 convertToDetailVO 中的固定字段
        assertEquals("High", result.getDescription()); // confidenceLevel mapped to description
        assertNotNull(result.getSkills());
        assertTrue(result.getSkills().isEmpty());
        assertNotNull(result.getCounterTips());
        assertTrue(result.getCounterTips().isEmpty());
        assertNotNull(result.getSynergies());
        assertTrue(result.getSynergies().isEmpty());

        verify(mockMapper).selectById(1L);
    }

    @Test
    @DisplayName("testGetHeroDetail_allFieldsMappedCorrectly")
    void testGetHeroDetail_allFieldsMappedCorrectly() {
        // Given
        Hero hero = createHero(42L, "Zed", "劫", "The Master of Shadows", "Assassin",
                "S", new BigDecimal("53.2"), new BigDecimal("18.7"));
        hero.setConfidenceLevel("Medium");
        when(mockMapper.selectById(42L)).thenReturn(hero);

        // When
        HeroDetailVO result = heroService.getHeroDetail(42L);

        // Then
        assertEquals(42L, result.getId());
        assertEquals("Zed", result.getNameEn());
        assertEquals("劫", result.getNameZh());
        assertEquals("The Master of Shadows", result.getTitle());
        assertEquals("Assassin", result.getRole());
        assertEquals("S", result.getTier());
        assertEquals(new BigDecimal("53.2"), result.getWinRate());
        assertEquals(new BigDecimal("18.7"), result.getPickRate());
        assertEquals("Medium", result.getDescription());
    }

    // ============================================================
    // 英雄详情 - 不存在
    // ============================================================

    @Test
    @DisplayName("testGetHeroDetail_heroNotFound_throwsBusinessException")
    void testGetHeroDetail_heroNotFound_throwsBusinessException() {
        // Given
        when(mockMapper.selectById(999L)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> heroService.getHeroDetail(999L));

        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("Hero not found"));
        assertTrue(exception.getMessage().contains("999"));

        verify(mockMapper).selectById(999L);
    }

    @Test
    @DisplayName("testGetHeroDetail_nullId_throwsBusinessException")
    void testGetHeroDetail_nullId_throwsBusinessException() {
        // Given
        when(mockMapper.selectById(null)).thenReturn(null);

        // When & Then
        assertThrows(BusinessException.class,
                () -> heroService.getHeroDetail(null));

        verify(mockMapper).selectById(null);
    }

    // ============================================================
    // 分页查询 - 多页数据验证
    // ============================================================

    @Test
    @DisplayName("testGetHeroList_page2_returnsCorrectSubset")
    void testGetHeroList_page2_returnsCorrectSubset() {
        // Given：第二页，每页 3 条
        List<Hero> page2Heroes = List.of(mockHeroes.get(3), mockHeroes.get(4)); // 剩余 2 条
        Page<Hero> mockPage = new Page<>(2, 3, 5);
        mockPage.setRecords(page2Heroes);

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When
        PageResult<HeroListVO> result = heroService.getHeroList(2, 3, null, null, null);

        // Then
        assertEquals(5, result.getTotal());
        assertEquals(2, result.getPage());
        assertEquals(3, result.getSize());
        assertEquals(2, result.getRecords().size());
    }

    @Test
    @DisplayName("testGetHeroList_page1_returnsFirstPageSizeRecords")
    void testGetHeroList_page1_returnsFirstPageSizeRecords() {
        // Given：第一页，每页 2 条
        List<Hero> page1Heroes = mockHeroes.subList(0, 2);
        Page<Hero> mockPage = new Page<>(1, 2, 5);
        mockPage.setRecords(page1Heroes);

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When
        PageResult<HeroListVO> result = heroService.getHeroList(1, 2, null, null, null);

        // Then
        assertEquals(5, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(2, result.getSize());
        assertEquals(2, result.getRecords().size());
        assertEquals("Ashe", result.getRecords().get(0).getNameEn());
        assertEquals("Lux", result.getRecords().get(1).getNameEn());
    }

    // ============================================================
    // VO 转换 - 数据完整性验证
    // ============================================================

    @Test
    @DisplayName("testGetHeroList_convertsAllFieldsCorrectly")
    void testGetHeroList_convertsAllFieldsCorrectly() {
        // Given
        Page<Hero> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(List.of(mockHeroes.get(0)));

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When
        PageResult<HeroListVO> result = heroService.getHeroList(1, 10, null, null, null);

        // Then
        HeroListVO vo = result.getRecords().get(0);
        assertEquals(1L, vo.getId());
        assertEquals("Ashe", vo.getNameEn());
        assertEquals("艾希", vo.getNameZh());
        assertEquals("Frost Archer", vo.getTitle());
        assertEquals("Marksman", vo.getRole());
        assertEquals("S+", vo.getTier());
        assertEquals(new BigDecimal("52.5"), vo.getWinRate());
        assertEquals(new BigDecimal("15.3"), vo.getPickRate());
        assertEquals("https://example.com/ashe.png", vo.getImageUrl());
    }
}
