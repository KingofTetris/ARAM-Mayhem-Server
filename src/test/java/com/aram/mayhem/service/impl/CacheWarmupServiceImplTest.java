package com.aram.mayhem.service.impl;

import com.aram.mayhem.dto.AugmentListVO;
import com.aram.mayhem.dto.HeroDetailVO;
import com.aram.mayhem.dto.HeroListVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.mapper.AugmentMapper;
import com.aram.mayhem.mapper.HeroMapper;
import com.aram.mayhem.service.AugmentService;
import com.aram.mayhem.service.HeroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CacheWarmupService 单元测试")
class CacheWarmupServiceImplTest {

    @Mock
    private HeroMapper heroMapper;

    @Mock
    private AugmentMapper augmentMapper;

    @Mock
    private HeroService heroService;

    @Mock
    private AugmentService augmentService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private CacheWarmupServiceImpl cacheWarmupService;

    private Hero createHero(Long id, String nameEn, BigDecimal winRate) {
        Hero hero = new Hero();
        hero.setId(id);
        hero.setNameEn(nameEn);
        hero.setNameZh(nameEn);
        hero.setTitle("Test Title");
        hero.setRole("Fighter");
        hero.setTier("S");
        hero.setWinRate(winRate);
        hero.setPickRate(new BigDecimal("10.00"));
        hero.setImageUrl("http://example.com/" + nameEn + ".png");
        hero.setIsVersionTrap(false);
        return hero;
    }

    private HeroDetailVO createHeroDetailVO(Long id, String nameEn) {
        HeroDetailVO vo = new HeroDetailVO();
        vo.setId(id);
        vo.setNameEn(nameEn);
        vo.setNameZh(nameEn);
        vo.setTier("S");
        vo.setWinRate(new BigDecimal("55.00"));
        return vo;
    }

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("warmupHeroCache - 正常预热 Top N 英雄缓存")
    void warmupHeroCache_success() {
        Hero hero1 = createHero(1L, "Aatrox", new BigDecimal("55.00"));
        Hero hero2 = createHero(2L, "Ahri", new BigDecimal("53.00"));
        List<Hero> topHeroes = List.of(hero1, hero2);

        when(heroMapper.selectList(any())).thenReturn(topHeroes);
        when(heroService.getHeroDetail(1L)).thenReturn(createHeroDetailVO(1L, "Aatrox"));
        when(heroService.getHeroDetail(2L)).thenReturn(createHeroDetailVO(2L, "Ahri"));

        List<Long> result = cacheWarmupService.warmupHeroCache(2);

        assertEquals(2, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(2L));
        verify(heroService).getHeroDetail(1L);
        verify(heroService).getHeroDetail(2L);
    }

    @Test
    @DisplayName("warmupHeroCache - 部分英雄预热失败仍继续")
    void warmupHeroCache_partialFailure() {
        Hero hero1 = createHero(1L, "Aatrox", new BigDecimal("55.00"));
        Hero hero2 = createHero(2L, "Ahri", new BigDecimal("53.00"));
        List<Hero> topHeroes = List.of(hero1, hero2);

        when(heroMapper.selectList(any())).thenReturn(topHeroes);
        when(heroService.getHeroDetail(1L)).thenReturn(createHeroDetailVO(1L, "Aatrox"));
        when(heroService.getHeroDetail(2L)).thenThrow(new RuntimeException("Redis connection failed"));

        List<Long> result = cacheWarmupService.warmupHeroCache(2);

        assertEquals(1, result.size());
        assertTrue(result.contains(1L));
    }

    @Test
    @DisplayName("warmupHeroCache - 空英雄列表返回空结果")
    void warmupHeroCache_emptyList() {
        when(heroMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<Long> result = cacheWarmupService.warmupHeroCache(50);

        assertTrue(result.isEmpty());
        verify(heroService, never()).getHeroDetail(anyLong());
    }

    @Test
    @DisplayName("warmupHeroCache - getHeroDetail 返回 null 时跳过")
    void warmupHeroCache_nullDetail() {
        Hero hero1 = createHero(1L, "Aatrox", new BigDecimal("55.00"));
        when(heroMapper.selectList(any())).thenReturn(List.of(hero1));
        when(heroService.getHeroDetail(1L)).thenReturn(null);

        List<Long> result = cacheWarmupService.warmupHeroCache(1);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("warmupAugmentCache - 正常预热符文缓存")
    void warmupAugmentCache_success() {
        AugmentListVO augment1 = new AugmentListVO();
        augment1.setId(1L);
        augment1.setNameZh("测试符文");
        PageResult<AugmentListVO> pageResult = new PageResult<>(1L, 1, 200, List.of(augment1));

        when(augmentService.getAugmentList(eq(1), eq(200), isNull(), isNull())).thenReturn(pageResult);

        int result = cacheWarmupService.warmupAugmentCache();

        assertEquals(1, result);
        verify(valueOperations).set(contains("augment:list:"), eq(pageResult), any());
    }

    @Test
    @DisplayName("warmupAugmentCache - 空符文列表返回 0")
    void warmupAugmentCache_emptyList() {
        PageResult<AugmentListVO> emptyPage = new PageResult<>(0L, 1, 200, Collections.emptyList());
        when(augmentService.getAugmentList(eq(1), eq(200), isNull(), isNull())).thenReturn(emptyPage);

        int result = cacheWarmupService.warmupAugmentCache();

        assertEquals(0, result);
    }

    @Test
    @DisplayName("warmupAugmentCache - 多页符文数据全部预热")
    void warmupAugmentCache_multiplePages() {
        List<AugmentListVO> page1Records = Collections.nCopies(200, new AugmentListVO());
        List<AugmentListVO> page2Records = Collections.nCopies(50, new AugmentListVO());
        PageResult<AugmentListVO> page1 = new PageResult<>(250L, 1, 200, page1Records);
        PageResult<AugmentListVO> page2 = new PageResult<>(250L, 2, 200, page2Records);

        when(augmentService.getAugmentList(eq(1), eq(200), isNull(), isNull())).thenReturn(page1);
        when(augmentService.getAugmentList(eq(2), eq(200), isNull(), isNull())).thenReturn(page2);

        int result = cacheWarmupService.warmupAugmentCache();

        assertEquals(250, result);
    }

    @Test
    @DisplayName("warmupAugmentCache - 查询异常时中断并返回已预热数量")
    void warmupAugmentCache_queryException() {
        when(augmentService.getAugmentList(eq(1), eq(200), isNull(), isNull()))
                .thenThrow(new RuntimeException("DB connection failed"));

        int result = cacheWarmupService.warmupAugmentCache();

        assertEquals(0, result);
    }

    @Test
    @DisplayName("warmupHeroListCache - 正常预热英雄列表缓存")
    void warmupHeroListCache_success() {
        Hero hero1 = createHero(1L, "Aatrox", new BigDecimal("55.00"));
        Hero hero2 = createHero(2L, "Ahri", new BigDecimal("53.00"));
        when(heroMapper.selectList(any())).thenReturn(List.of(hero1, hero2));

        int result = cacheWarmupService.warmupHeroListCache();

        assertEquals(2, result);
        verify(valueOperations).set(eq("hero:list:all"), any(), any());
    }

    @Test
    @DisplayName("warmupHeroListCache - 空列表返回 0")
    void warmupHeroListCache_emptyList() {
        when(heroMapper.selectList(any())).thenReturn(Collections.emptyList());

        int result = cacheWarmupService.warmupHeroListCache();

        assertEquals(0, result);
    }

    @Test
    @DisplayName("cleanupStaleCache - 清理无效英雄缓存")
    void cleanupStaleCache_staleHeroCache() {
        when(redisTemplate.keys("heroDetail::*")).thenReturn(Set.of("heroDetail::999"));
        Hero validHero = new Hero();
        validHero.setId(1L);
        when(heroMapper.selectList(isNull())).thenReturn(List.of(validHero));
        when(redisTemplate.delete("heroDetail::999")).thenReturn(true);

        int result = cacheWarmupService.cleanupStaleCache();

        assertEquals(1, result);
        verify(redisTemplate).delete("heroDetail::999");
    }

    @Test
    @DisplayName("cleanupStaleCache - 无过期缓存返回 0")
    void cleanupStaleCache_noStaleCache() {
        when(redisTemplate.keys("heroDetail::*")).thenReturn(Set.of("heroDetail::1"));
        Hero validHero = new Hero();
        validHero.setId(1L);
        when(heroMapper.selectList(isNull())).thenReturn(List.of(validHero));
        when(redisTemplate.keys("augment:list:*")).thenReturn(Collections.emptySet());

        int result = cacheWarmupService.cleanupStaleCache();

        assertEquals(0, result);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("cleanupStaleCache - keys 返回 null 时安全处理")
    void cleanupStaleCache_nullKeys() {
        when(redisTemplate.keys("heroDetail::*")).thenReturn(null);
        when(redisTemplate.keys("augment:list:*")).thenReturn(null);

        int result = cacheWarmupService.cleanupStaleCache();

        assertEquals(0, result);
    }
}
