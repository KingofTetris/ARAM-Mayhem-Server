package com.aram.mayhem.service.impl;

import com.aram.mayhem.dto.AugmentListVO;
import com.aram.mayhem.dto.HeroDetailVO;
import com.aram.mayhem.dto.HeroListVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.mapper.AugmentMapper;
import com.aram.mayhem.mapper.HeroMapper;
import com.aram.mayhem.service.CacheWarmupService;
import com.aram.mayhem.service.HeroService;
import com.aram.mayhem.service.AugmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheWarmupServiceImpl implements CacheWarmupService {

    private static final String HERO_DETAIL_CACHE_PREFIX = "heroDetail";
    private static final String AUGMENT_LIST_CACHE_PREFIX = "augment:list:";
    private static final String HERO_LIST_CACHE_KEY = "hero:list:all";
    private static final Duration HERO_CACHE_TTL = Duration.ofHours(6);
    private static final Duration AUGMENT_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration HERO_LIST_CACHE_TTL = Duration.ofHours(6);
    private static final int AUGMENT_PAGE_SIZE = 200;

    private final HeroMapper heroMapper;
    private final AugmentMapper augmentMapper;
    private final HeroService heroService;
    private final AugmentService augmentService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<Long> warmupHeroCache(int topN) {
        log.info("[WARMUP] hero cache warmup started | topN={}", topN);
        long startTime = System.currentTimeMillis();

        LambdaQueryWrapper<Hero> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Hero::getWinRate);
        wrapper.last("LIMIT " + topN);
        List<Hero> topHeroes = heroMapper.selectList(wrapper);

        List<Long> warmedUpIds = new ArrayList<>();
        int success = 0;
        int failed = 0;

        for (Hero hero : topHeroes) {
            try {
                HeroDetailVO detail = heroService.getHeroDetail(hero.getId());
                if (detail != null) {
                    warmedUpIds.add(hero.getId());
                    success++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("[WARMUP] failed to warmup hero cache | id={} | name={} | error={}",
                        hero.getId(), hero.getNameEn(), e.getMessage());
            }
        }

        long durationMs = System.currentTimeMillis() - startTime;
        log.info("[WARMUP] hero cache warmup completed | total={} | success={} | failed={} | duration={}ms",
                topHeroes.size(), success, failed, durationMs);
        return warmedUpIds;
    }

    @Override
    public int warmupAugmentCache() {
        log.info("[WARMUP] augment cache warmup started");
        long startTime = System.currentTimeMillis();

        int totalWarmed = 0;
        int page = 1;

        while (true) {
            try {
                PageResult<AugmentListVO> result = augmentService.getAugmentList(page, AUGMENT_PAGE_SIZE, null, null);
                if (result.getRecords() == null || result.getRecords().isEmpty()) {
                    break;
                }
                totalWarmed += result.getRecords().size();

                String cacheKey = AUGMENT_LIST_CACHE_PREFIX + "null:null:" + page + ":" + AUGMENT_PAGE_SIZE;
                redisTemplate.opsForValue().set(cacheKey, result, AUGMENT_CACHE_TTL);

                if (result.getRecords().size() < AUGMENT_PAGE_SIZE) {
                    break;
                }
                page++;
            } catch (Exception e) {
                log.warn("[WARMUP] failed to warmup augment cache at page={} | error={}", page, e.getMessage());
                break;
            }
        }

        long durationMs = System.currentTimeMillis() - startTime;
        log.info("[WARMUP] augment cache warmup completed | totalWarmed={} | duration={}ms", totalWarmed, durationMs);
        return totalWarmed;
    }

    @Override
    public int warmupHeroListCache() {
        log.info("[WARMUP] hero list cache warmup started");
        long startTime = System.currentTimeMillis();

        int totalWarmed = 0;

        LambdaQueryWrapper<Hero> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Hero::getWinRate);
        List<Hero> allHeroes = heroMapper.selectList(wrapper);

        List<HeroListVO> heroListVOs = allHeroes.stream()
                .map(this::convertToListVO)
                .toList();

        redisTemplate.opsForValue().set(HERO_LIST_CACHE_KEY, heroListVOs, HERO_LIST_CACHE_TTL);
        totalWarmed = heroListVOs.size();

        long durationMs = System.currentTimeMillis() - startTime;
        log.info("[WARMUP] hero list cache warmup completed | totalWarmed={} | duration={}ms", totalWarmed, durationMs);
        return totalWarmed;
    }

    @Override
    public int cleanupStaleCache() {
        log.info("[WARMUP] stale cache cleanup started");
        long startTime = System.currentTimeMillis();

        int cleaned = 0;

        Set<String> heroDetailKeys = redisTemplate.keys(HERO_DETAIL_CACHE_PREFIX + "::*");
        if (heroDetailKeys != null && !heroDetailKeys.isEmpty()) {
            List<Hero> allHeroes = heroMapper.selectList(null);
            Set<Long> validHeroIds = allHeroes.stream()
                    .map(Hero::getId)
                    .collect(java.util.stream.Collectors.toSet());

            for (String key : heroDetailKeys) {
                String idPart = key.substring(key.lastIndexOf("::") + 2);
                try {
                    Long cachedId = Long.parseLong(idPart);
                    if (!validHeroIds.contains(cachedId)) {
                        redisTemplate.delete(key);
                        cleaned++;
                        log.debug("[WARMUP] deleted stale hero cache | key={}", key);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        Set<String> augmentListKeys = redisTemplate.keys(AUGMENT_LIST_CACHE_PREFIX + "*");
        if (augmentListKeys != null && !augmentListKeys.isEmpty()) {
            long expiredCount = augmentListKeys.stream()
                    .filter(key -> redisTemplate.getExpire(key) != null && redisTemplate.getExpire(key) < 0)
                    .count();
            if (expiredCount > 0) {
                log.info("[WARMUP] found {} expired augment cache keys (already handled by TTL)", expiredCount);
            }
        }

        long durationMs = System.currentTimeMillis() - startTime;
        log.info("[WARMUP] stale cache cleanup completed | cleaned={} | duration={}ms", cleaned, durationMs);
        return cleaned;
    }

    private HeroListVO convertToListVO(Hero hero) {
        HeroListVO vo = new HeroListVO();
        vo.setId(hero.getId());
        vo.setNameEn(hero.getNameEn());
        vo.setNameZh(hero.getNameZh());
        vo.setTitle(hero.getTitle());
        vo.setRole(hero.getRole());
        vo.setTier(hero.getTier());
        vo.setWinRate(hero.getWinRate());
        vo.setPickRate(hero.getPickRate());
        vo.setImageUrl(hero.getImageUrl());
        vo.setIsVersionTrap(hero.getIsVersionTrap());
        return vo;
    }
}
