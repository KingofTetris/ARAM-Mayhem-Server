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

@Service
public class HeroServiceImpl implements HeroService {

    private static final Logger log = LoggerFactory.getLogger(HeroServiceImpl.class);

    private final HeroMapper heroMapper;

    public HeroServiceImpl(HeroMapper heroMapper) {
        this.heroMapper = heroMapper;
    }

    @Override
    public PageResult<HeroListVO> getHeroList(int page, int size, String keyword, String tier, String sortBy) {
        LambdaQueryWrapper<Hero> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Hero::getNameEn, keyword)
                    .or()
                    .like(Hero::getNameZh, keyword)
                    .or()
                    .like(Hero::getTitle, keyword));
        }

        if (StringUtils.hasText(tier)) {
            queryWrapper.eq(Hero::getTier, tier);
        }

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

        List<HeroListVO> records = result.getRecords().stream()
                .map(this::convertToListVO)
                .toList();

        return new PageResult<>(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), records);
    }

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
