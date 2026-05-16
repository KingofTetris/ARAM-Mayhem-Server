package com.aram.mayhem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aram.mayhem.dto.HeroDetailVO;
import com.aram.mayhem.dto.HeroListVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.common.BusinessException;
import com.aram.mayhem.mapper.HeroMapper;
import com.aram.mayhem.service.HeroService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 英雄服务实现类
 *
 * 功能：英雄列表分页查询、英雄详情获取（含缓存）
 * 缓存：heroDetail 缓存（key = 英雄ID）
 * 关联：HeroMapper, HeroService
 */
@Service
public class HeroServiceImpl implements HeroService {

    private static final Logger log = LoggerFactory.getLogger(HeroServiceImpl.class);

    private final HeroMapper heroMapper;

    public HeroServiceImpl(HeroMapper heroMapper) {
        this.heroMapper = heroMapper;
    }

    /**
     * 获取英雄列表（英雄模块）
     *
     * 作用：分页查询英雄列表，支持关键词搜索、梯级筛选和多种排序方式
     * 梯级等级：S+/S/A/B/C（从强到弱）
     * 排序字段：winRate(胜率)/pickRate(选取率)/tier(梯级)/name(名称)
     * 搜索范围：英文名、中文名、称号
     *
     * @param page    页码（从1开始）
     * @param size    每页数量
     * @param keyword 搜索关键词（匹配英文名/中文名/称号）
     * @param tier    梯级筛选（S+/S/A/B/C）
     * @param sortBy  排序字段
     * @return PageResult<HeroListVO> 分页英雄列表
     */
    @Override
    public PageResult<HeroListVO> getHeroList(int page, int size, String keyword, String tier, String sortBy) {
        LambdaQueryWrapper<Hero> queryWrapper = new LambdaQueryWrapper<>();

        // 关键词搜索（支持英文名、中文名、称号）
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Hero::getNameEn, keyword)
                    .or()
                    .like(Hero::getNameZh, keyword)
                    .or()
                    .like(Hero::getTitle, keyword));
        }

        // 梯级筛选
        if (StringUtils.hasText(tier)) {
            queryWrapper.eq(Hero::getTier, tier);
        }

        // 排序逻辑
        if (StringUtils.hasText(sortBy)) {
            switch (sortBy) {
                case "winRate" -> queryWrapper.orderByDesc(Hero::getWinRate);
                case "pickRate" -> queryWrapper.orderByDesc(Hero::getPickRate);
                case "tier" -> queryWrapper.orderByAsc(Hero::getTier);
                case "name" -> queryWrapper.orderByAsc(Hero::getNameEn);
                default -> queryWrapper.orderByDesc(Hero::getWinRate);
            }
        } else {
            queryWrapper.orderByDesc(Hero::getWinRate);
        }

        Page<Hero> heroPage = new Page<>(page, size);
        Page<Hero> result = heroMapper.selectPage(heroPage, queryWrapper);

        // 转换为VO
        List<HeroListVO> records = result.getRecords().stream()
                .map(this::convertToListVO)
                .toList();

        return new PageResult<>(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), records);
    }

    /**
     * 获取英雄详情（英雄模块）
     *
     * 作用：根据英雄ID获取详细信息，包括技能、属性、克制关系、推荐出装等
     * 缓存：使用@Cacheable缓存，key为英雄ID
     *
     * @param id 英雄ID
     * @return HeroDetailVO 英雄详情
     * @throws BusinessException 英雄不存在时抛出404错误
     */
    @Override
    @Cacheable(value = "heroDetail", key = "#id", unless = "#result == null")
    public HeroDetailVO getHeroDetail(Long id) {
        log.info("Getting hero detail: id={}", id);
        Hero hero = heroMapper.selectById(id);
        if (hero == null) {
            log.warn("Hero not found: id={}", id);
            throw new BusinessException(404, "Hero not found with id: " + id);
        }
        return convertToDetailVO(hero);
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

    private HeroDetailVO convertToDetailVO(Hero hero) {
        HeroDetailVO vo = new HeroDetailVO();
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
        vo.setDescription(hero.getDescription());
        vo.setAvgKills(hero.getAvgKills());
        vo.setAvgDeaths(hero.getAvgDeaths());
        vo.setAvgAssists(hero.getAvgAssists());
        vo.setRecommendedBuild(hero.getRecommendedBuild());

        if (hero.getSkills() != null) {
            List<HeroDetailVO.SkillInfo> skillInfos = hero.getSkills().stream()
                    .map(skill -> {
                        HeroDetailVO.SkillInfo info = new HeroDetailVO.SkillInfo();
                        info.setKey(skill.getKey());
                        info.setName(skill.getName());
                        info.setDescription(skill.getDescription());
                        return info;
                    })
                    .toList();
            vo.setSkills(skillInfos);
        } else {
            vo.setSkills(List.of());
        }

        vo.setCounterTips(hero.getCounterTips() != null ? hero.getCounterTips() : List.of());
        vo.setSynergies(hero.getSynergies() != null ? hero.getSynergies() : List.of());

        return vo;
    }
}
