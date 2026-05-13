package com.aram.mayhem.service.impl;

import com.aram.mayhem.dto.AugmentListVO;
import com.aram.mayhem.dto.AugmentVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.mapper.AugmentMapper;
import com.aram.mayhem.service.AugmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AugmentServiceImpl implements AugmentService {

    private static final String CACHE_KEY_PREFIX = "augment:list:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final AugmentMapper augmentMapper;
    private final StringRedisTemplate redisTemplate;

    public AugmentServiceImpl(AugmentMapper augmentMapper, StringRedisTemplate redisTemplate) {
        this.augmentMapper = augmentMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public PageResult<AugmentListVO> getAugmentList(int page, int size, String quality, String synergySet) {
        String cacheKey = CACHE_KEY_PREFIX + quality + ":" + synergySet + ":" + page + ":" + size;

        Page<Augment> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Augment> wrapper = new LambdaQueryWrapper<>();

        if (quality != null && !quality.isEmpty()) {
            wrapper.eq(Augment::getQuality, quality);
        }
        if (synergySet != null && !synergySet.isEmpty()) {
            wrapper.and(w -> w
                    .eq(Augment::getSynergySet, synergySet)
                    .or()
                    .eq(Augment::getSynergySet2, synergySet)
                    .or()
                    .eq(Augment::getSynergySet3, synergySet)
            );
        }

        wrapper.orderByDesc(Augment::getWinRate);
        Page<Augment> result = augmentMapper.selectPage(pageParam, wrapper);

        List<AugmentListVO> voList = result.getRecords().stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());

        return new PageResult<>(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), voList);
    }

    @Override
    public AugmentVO getAugmentDetail(Long id) {
        Augment augment = augmentMapper.selectById(id);
        if (augment == null) {
            return null;
        }
        return convertToVO(augment);
    }

    private AugmentListVO convertToListVO(Augment augment) {
        AugmentListVO vo = new AugmentListVO();
        vo.setId(augment.getId());
        vo.setNameZh(augment.getNameZh());
        vo.setNameEn(augment.getNameEn());
        vo.setQuality(augment.getQuality());
        vo.setSynergySet(augment.getSynergySet());
        vo.setIconUrl(augment.getIconUrl());
        vo.setWinRate(augment.getWinRate());
        vo.setPickRate(augment.getPickRate());
        vo.setAvgPlacement(augment.getAvgPlacement());
        vo.setTier(augment.getTier());
        return vo;
    }

    private AugmentVO convertToVO(Augment augment) {
        AugmentVO vo = new AugmentVO();
        BeanUtils.copyProperties(augment, vo);
        return vo;
    }
}
