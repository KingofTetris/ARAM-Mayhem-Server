package com.aram.mayhem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aram.mayhem.common.BusinessException;
import com.aram.mayhem.dto.BulletinDetailVO;
import com.aram.mayhem.dto.BulletinListVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.entity.Bulletin;
import com.aram.mayhem.mapper.BulletinMapper;
import com.aram.mayhem.service.BulletinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class BulletinServiceImpl implements BulletinService {

    private static final Logger log = LoggerFactory.getLogger(BulletinServiceImpl.class);

    private final BulletinMapper bulletinMapper;

    public BulletinServiceImpl(BulletinMapper bulletinMapper) {
        this.bulletinMapper = bulletinMapper;
    }

    @Override
    public PageResult<BulletinListVO> getBulletinList(String type, int page, int size) {
        log.info("Getting bulletin list: type={}, page={}, size={}", type, page, size);

        LambdaQueryWrapper<Bulletin> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(type)) {
            queryWrapper.eq(Bulletin::getType, type);
        }

        queryWrapper.orderByDesc(Bulletin::getIsPinned)
                .orderByDesc(Bulletin::getCreatedAt);

        Page<Bulletin> bulletinPage = new Page<>(page, size);
        Page<Bulletin> result = bulletinMapper.selectPage(bulletinPage, queryWrapper);

        List<BulletinListVO> records = result.getRecords().stream()
                .map(this::convertToListVO)
                .toList();

        return new PageResult<>(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), records);
    }

    @Override
    public List<BulletinListVO> getLatestBulletins(int limit) {
        log.info("Getting latest bulletins: limit={}", limit);

        LambdaQueryWrapper<Bulletin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Bulletin::getIsPinned)
                .orderByDesc(Bulletin::getCreatedAt)
                .last("LIMIT " + limit);

        List<Bulletin> bulletins = bulletinMapper.selectList(queryWrapper);
        return bulletins.stream()
                .map(this::convertToListVO)
                .toList();
    }

    @Override
    public BulletinDetailVO getBulletinDetail(Long id) {
        log.info("Getting bulletin detail: id={}", id);

        Bulletin bulletin = bulletinMapper.selectById(id);
        if (bulletin == null) {
            log.warn("Bulletin not found: id={}", id);
            throw new BusinessException(404, "Bulletin not found with id: " + id);
        }

        return convertToDetailVO(bulletin);
    }

    private BulletinListVO convertToListVO(Bulletin bulletin) {
        BulletinListVO vo = new BulletinListVO();
        vo.setId(bulletin.getId());
        vo.setType(bulletin.getType());
        vo.setTitle(bulletin.getTitle());
        vo.setContent(bulletin.getContent());
        vo.setImageUrl(bulletin.getImageUrl());
        vo.setIsPinned(bulletin.getIsPinned());
        vo.setPublishedAt(bulletin.getPublishedAt());
        vo.setCreatedAt(bulletin.getCreatedAt());
        return vo;
    }

    private BulletinDetailVO convertToDetailVO(Bulletin bulletin) {
        BulletinDetailVO vo = new BulletinDetailVO();
        vo.setId(bulletin.getId());
        vo.setType(bulletin.getType());
        vo.setTitle(bulletin.getTitle());
        vo.setContent(bulletin.getContent());
        vo.setImageUrl(bulletin.getImageUrl());
        vo.setIsPinned(bulletin.getIsPinned());
        vo.setPublishedAt(bulletin.getPublishedAt());
        vo.setCreatedAt(bulletin.getCreatedAt());
        return vo;
    }
}
