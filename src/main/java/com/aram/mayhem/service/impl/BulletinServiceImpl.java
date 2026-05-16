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

/**
 * 公告服务实现类
 *
 * 功能：公告列表分页查询、最新公告获取、公告详情查看
 * 关联：BulletinMapper, BulletinService
 */
@Service
public class BulletinServiceImpl implements BulletinService {

    private static final Logger log = LoggerFactory.getLogger(BulletinServiceImpl.class);

    private final BulletinMapper bulletinMapper;

    public BulletinServiceImpl(BulletinMapper bulletinMapper) {
        this.bulletinMapper = bulletinMapper;
    }

    /**
     * 获取公告列表（公告模块）
     *
     * 作用：分页查询公告列表，支持按类型筛选
     * 公告类型：version(版本更新)/event(活动)/notice(公告)
     * 排序规则：置顶公告优先，然后按发布时间降序
     *
     * @param type 公告类型（可选）
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return PageResult<BulletinListVO> 分页公告列表
     */
    @Override
    public PageResult<BulletinListVO> getBulletinList(String type, int page, int size) {
        log.info("Getting bulletin list: type={}, page={}, size={}", type, page, size);

        LambdaQueryWrapper<Bulletin> queryWrapper = new LambdaQueryWrapper<>();

        // 类型筛选
        if (StringUtils.hasText(type)) {
            queryWrapper.eq(Bulletin::getType, type);
        }

        // 排序：置顶优先，然后按时间降序
        queryWrapper.orderByDesc(Bulletin::getIsPinned)
                .orderByDesc(Bulletin::getCreatedAt);

        Page<Bulletin> bulletinPage = new Page<>(page, size);
        Page<Bulletin> result = bulletinMapper.selectPage(bulletinPage, queryWrapper);

        // 转换为VO
        List<BulletinListVO> records = result.getRecords().stream()
                .map(this::convertToListVO)
                .toList();

        return new PageResult<>(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), records);
    }

    /**
     * 获取最新公告（公告模块）
     *
     * 作用：获取最新N条公告，用于首页轮播展示
     * 默认获取3条，可通过limit参数调整
     *
     * @param limit 获取数量（默认3条）
     * @return List<BulletinListVO> 最新公告列表
     */
    @Override
    public List<BulletinListVO> getLatestBulletins(int limit) {
        log.info("Getting latest bulletins: limit={}", limit);

        LambdaQueryWrapper<Bulletin> queryWrapper = new LambdaQueryWrapper<>();
        // 排序：置顶优先，然后按时间降序
        queryWrapper.orderByDesc(Bulletin::getIsPinned)
                .orderByDesc(Bulletin::getCreatedAt)
                .last("LIMIT " + limit);

        List<Bulletin> bulletins = bulletinMapper.selectList(queryWrapper);
        return bulletins.stream()
                .map(this::convertToListVO)
                .toList();
    }

    /**
     * 获取公告详情（公告模块）
     *
     * 作用：根据公告ID获取详细内容，包括标题、内容、发布时间等
     *
     * @param id 公告ID
     * @return BulletinDetailVO 公告详情
     * @throws BusinessException 公告不存在时抛出404错误
     */
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
