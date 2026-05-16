package com.aram.mayhem.service;

import com.aram.mayhem.dto.AugmentListVO;
import com.aram.mayhem.dto.AugmentRecommendRequest;
import com.aram.mayhem.dto.AugmentRecommendResponse;
import com.aram.mayhem.dto.AugmentVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.dto.SynergyProgressResponse;

import java.util.List;

/**
 * 强化符文服务接口
 *
 * 功能：符文列表查询、符文详情、套装进度计算、智能推荐
 * 实现：AugmentServiceImpl
 */
public interface AugmentService {

    /**
     * 获取符文列表（支持品质和套装筛选）
     *
     * @param page        页码
     * @param size        每页数量
     * @param quality     品质筛选（银色/金色/棱彩）
     * @param synergySet  套装筛选
     * @return 分页符文列表
     */
    PageResult<AugmentListVO> getAugmentList(int page, int size, String quality, String synergySet);

    /**
     * 获取符文详情
     *
     * @param id 符文ID
     * @return 符文详情
     */
    AugmentVO getAugmentDetail(Long id);

    /**
     * 计算已选符文的套装激活进度
     *
     * @param augmentIds 已选符文ID（逗号分隔）
     * @return 各套装的激活进度列表
     */
    List<SynergyProgressResponse> getSynergyProgress(String augmentIds);

    /**
     * 基于英雄和已选符文，智能推荐下一个符文
     *
     * @param request 推荐请求（含英雄ID和已选符文列表）
     * @return 推荐符文列表（按评分降序）
     */
    List<AugmentRecommendResponse> getRecommendations(AugmentRecommendRequest request);
}