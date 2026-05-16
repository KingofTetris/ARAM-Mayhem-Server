package com.aram.mayhem.service;

import com.aram.mayhem.dto.StrategyDetailVO;
import com.aram.mayhem.dto.StrategyListVO;

import java.util.List;

/**
 * 攻略服务接口
 *
 * 功能：攻略列表查询、攻略详情、攻略创建、投票管理
 * 实现：StrategyServiceImpl
 */
public interface StrategyService {

    /**
     * 获取攻略列表（支持排序和分页）
     *
     * @param sort 排序方式（latest/popular）
     * @param page 页码
     * @param size 每页数量
     * @return 攻略列表
     */
    List<StrategyListVO> getStrategyList(String sort, int page, int size);

    /**
     * 获取攻略详情
     *
     * @param id 攻略ID
     * @return 攻略详情（含符文和装备列表）
     */
    StrategyDetailVO getStrategyDetail(Long id);

    /**
     * 创建攻略
     *
     * @param userId      发布用户ID
     * @param heroId      关联英雄ID
     * @param title       攻略标题
     * @param description 攻略正文
     * @param augmentIds  推荐符文ID列表
     * @param itemIds     推荐装备ID列表
     * @return 创建后的攻略详情
     */
    StrategyDetailVO createStrategy(Long userId, Long heroId, String title, String description,
                                     List<Long> augmentIds, List<Long> itemIds);

    /**
     * 获取指定用户的攻略列表
     *
     * @param userId 用户ID
     * @return 该用户发布的攻略列表
     */
    List<StrategyListVO> getUserStrategies(Long userId);

    /** 点赞攻略 */
    void upvoteStrategy(Long strategyId);

    /** 踩攻略 */
    void downvoteStrategy(Long strategyId);

    /**
     * 投票（点赞或踩，已投票则更新）
     *
     * @param strategyId 攻略ID
     * @param userId     用户ID
     * @param voteType   投票类型（UP/DOWN）
     */
    void vote(Long strategyId, Long userId, String voteType);

    /**
     * 取消投票
     *
     * @param strategyId 攻略ID
     * @param userId     用户ID
     */
    void cancelVote(Long strategyId, Long userId);

    /**
     * 删除攻略
     *
     * 作用：删除指定攻略及其关联数据（符文关联、装备关联、投票记录）
     * 权限：仅攻略作者可删除
     *
     * @param strategyId 攻略ID
     * @param userId     当前用户ID（用于权限校验）
     * @throws IllegalArgumentException 攻略不存在或无权删除时抛出
     */
    void deleteStrategy(Long strategyId, Long userId);
}