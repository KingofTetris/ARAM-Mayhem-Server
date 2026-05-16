package com.aram.mayhem.service;

import com.aram.mayhem.dto.BulletinDetailVO;
import com.aram.mayhem.dto.BulletinListVO;
import com.aram.mayhem.dto.PageResult;

import java.util.List;

/**
 * 公告服务接口
 *
 * 功能：公告列表分页查询、最新公告获取、公告详情查看
 * 实现：BulletinServiceImpl
 */
public interface BulletinService {

    /**
     * 获取公告列表（支持类型筛选）
     *
     * @param type 公告类型筛选（version/event/notice，null=全部）
     * @param page 页码
     * @param size 每页数量
     * @return 分页公告列表
     */
    PageResult<BulletinListVO> getBulletinList(String type, int page, int size);

    /**
     * 获取最新公告列表
     *
     * @param limit 返回数量上限
     * @return 最新公告列表（按发布时间倒序）
     */
    List<BulletinListVO> getLatestBulletins(int limit);

    /**
     * 获取公告详情
     *
     * @param id 公告ID
     * @return 公告详情
     */
    BulletinDetailVO getBulletinDetail(Long id);
}
