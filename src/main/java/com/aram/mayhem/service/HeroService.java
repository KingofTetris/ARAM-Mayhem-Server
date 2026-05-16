package com.aram.mayhem.service;

import com.aram.mayhem.dto.HeroDetailVO;
import com.aram.mayhem.dto.HeroListVO;
import com.aram.mayhem.dto.PageResult;

/**
 * 英雄服务接口
 *
 * 功能：英雄列表分页查询、英雄详情获取
 * 实现：HeroServiceImpl
 */
public interface HeroService {

    /**
     * 获取英雄列表（支持关键词搜索、梯级筛选、排序）
     *
     * @param page    页码（从1开始）
     * @param size    每页数量
     * @param keyword 搜索关键词（匹配英文名/中文名/称号）
     * @param tier    梯级筛选（S+/S/A/B/C）
     * @param sortBy  排序字段（winRate/pickRate/tier/name）
     * @return 分页英雄列表
     */
    PageResult<HeroListVO> getHeroList(int page, int size, String keyword, String tier, String sortBy);

    /**
     * 获取英雄详情（含技能、克制技巧、协同英雄、推荐出装）
     *
     * @param id 英雄ID
     * @return 英雄详情
     */
    HeroDetailVO getHeroDetail(Long id);
}
