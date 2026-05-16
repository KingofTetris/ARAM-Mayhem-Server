package com.aram.mayhem.service.impl;

import com.aram.mayhem.dto.*;
import com.aram.mayhem.entity.*;
import com.aram.mayhem.mapper.*;
import com.aram.mayhem.service.StrategyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 攻略服务实现类
 *
 * 功能：攻略CRUD、投票管理（点赞/踩/取消）
 * 关联：StrategyMapper, StrategyAugmentMapper, StrategyItemMapper, VoteMapper, AugmentMapper, UserMapper
 * 事务：创建攻略需同时写入攻略主体+符文关联+装备关联
 */
@Service
public class StrategyServiceImpl implements StrategyService {

    private static final Logger log = LoggerFactory.getLogger(StrategyServiceImpl.class);

    private final StrategyMapper strategyMapper;
    private final StrategyAugmentMapper strategyAugmentMapper;
    private final StrategyItemMapper strategyItemMapper;
    private final HeroMapper heroMapper;
    private final UserMapper userMapper;
    private final VoteMapper voteMapper;
    private final AugmentMapper augmentMapper;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public StrategyServiceImpl(StrategyMapper strategyMapper,
                                StrategyAugmentMapper strategyAugmentMapper,
                                StrategyItemMapper strategyItemMapper,
                                HeroMapper heroMapper,
                                UserMapper userMapper,
                                VoteMapper voteMapper,
                                AugmentMapper augmentMapper,
                                StringRedisTemplate redisTemplate) {
        this.strategyMapper = strategyMapper;
        this.strategyAugmentMapper = strategyAugmentMapper;
        this.strategyItemMapper = strategyItemMapper;
        this.heroMapper = heroMapper;
        this.userMapper = userMapper;
        this.voteMapper = voteMapper;
        this.augmentMapper = augmentMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取攻略列表（社区模块）
     *
     * 作用：分页查询攻略列表，支持按热度或时间排序
     * 排序规则：hot-按点赞差排序，latest-按发布时间排序
     * 参数校验：page最小1，size最大100
     *
     * @param sort 排序方式（hot/最新）
     * @param page 页码（从1开始）
     * @param size 每页数量（最大100）
     * @return List<StrategyListVO> 攻略列表
     */
    @Override
    public List<StrategyListVO> getStrategyList(String sort, int page, int size) {
        // 参数边界校验
        if (page <= 0) page = 1;
        if (size <= 0) size = 10;
        if (size > 100) size = 100;

        Page<Strategy> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Strategy> wrapper = new LambdaQueryWrapper<>();

        // 排序逻辑：hot按点赞差，其他按时间
        if ("hot".equalsIgnoreCase(sort)) {
            wrapper.orderByDesc(s -> s.getUpvotes() - s.getDownvotes());
        } else {
            wrapper.orderByDesc(Strategy::getCreatedAt);
        }

        Page<Strategy> result = strategyMapper.selectPage(pageParam, wrapper);

        // 转换为VO并返回
        return result.getRecords().stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取攻略详情（社区模块）
     *
     * 作用：根据攻略ID获取详细信息，包括关联的英雄、符文、装备等
     *
     * @param id 攻略ID
     * @return StrategyDetailVO 攻略详情，不存在返回null
     */
    @Override
    public StrategyDetailVO getStrategyDetail(Long id) {
        Strategy strategy = strategyMapper.selectById(id);
        if (strategy == null) {
            return null;
        }
        return convertToDetailVO(strategy);
    }

    /**
     * 创建攻略（社区模块）
     *
     * 作用：用户发布新的游戏攻略，包含英雄、标题、描述、符文和装备关联
     * 事务：需同时写入攻略主体+符文关联+装备关联（@Transactional）
     *
     * @param userId      发布用户ID
     * @param heroId      关联英雄ID
     * @param title       攻略标题
     * @param description 攻略正文（至少10字）
     * @param augmentIds  推荐符文ID列表
     * @param itemIds     推荐装备ID列表
     * @return StrategyDetailVO 创建后的攻略详情
     * @throws IllegalArgumentException 参数校验失败时抛出
     */
    @Override
    @Transactional
    public StrategyDetailVO createStrategy(Long userId, Long heroId, String title, String description,
                                           List<Long> augmentIds, List<Long> itemIds) {
        log.info("Creating strategy: userId={}, heroId={}, title={}", userId, heroId, title);

        // 参数校验
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (description == null || description.trim().length() < 10) {
            throw new IllegalArgumentException("描述至少需要10个字");
        }

        // 创建攻略主体
        Strategy strategy = new Strategy();
        strategy.setUserId(userId);
        strategy.setHeroId(heroId);
        strategy.setTitle(title.trim());
        strategy.setDescription(description.trim());
        strategy.setUpvotes(0);
        strategy.setDownvotes(0);

        strategyMapper.insert(strategy);
        log.info("Strategy created: strategyId={}, userId={}", strategy.getId(), userId);

        // 关联符文
        if (augmentIds != null && !augmentIds.isEmpty()) {
            for (Long augmentId : augmentIds) {
                StrategyAugment sa = new StrategyAugment();
                sa.setStrategyId(strategy.getId());
                sa.setAugmentId(augmentId);
                strategyAugmentMapper.insert(sa);
            }
        }

        // 关联装备
        if (itemIds != null && !itemIds.isEmpty()) {
            for (Long itemId : itemIds) {
                StrategyItem si = new StrategyItem();
                si.setStrategyId(strategy.getId());
                si.setItemName("Item-" + itemId);
                si.setItemCategory("build");
                si.setSortOrder(0);
                strategyItemMapper.insert(si);
            }
        }

        return convertToDetailVO(strategy);
    }

    /**
     * 获取用户发布的攻略列表（社区模块）
     *
     * 作用：获取指定用户发布的所有攻略
     *
     * @param userId 用户ID
     * @return List<StrategyListVO> 用户发布的攻略列表
     */
    @Override
    public List<StrategyListVO> getUserStrategies(Long userId) {
        LambdaQueryWrapper<Strategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Strategy::getUserId, userId)
                .orderByDesc(Strategy::getCreatedAt);

        return strategyMapper.selectList(wrapper).stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());
    }

    /**
     * 点赞攻略（社区模块）
     *
     * 作用：增加攻略点赞数
     * 事务：确保计数一致性
     *
     * @param strategyId 攻略ID
     */
    @Override
    @Transactional
    public void upvoteStrategy(Long strategyId) {
        Strategy strategy = strategyMapper.selectById(strategyId);
        if (strategy != null) {
            strategy.setUpvotes(strategy.getUpvotes() + 1);
            strategyMapper.updateById(strategy);
        }
    }

    /**
     * 踩攻略（社区模块）
     *
     * 作用：增加攻略踩数
     * 事务：确保计数一致性
     *
     * @param strategyId 攻略ID
     */
    @Override
    @Transactional
    public void downvoteStrategy(Long strategyId) {
        Strategy strategy = strategyMapper.selectById(strategyId);
        if (strategy != null) {
            strategy.setDownvotes(strategy.getDownvotes() + 1);
            strategyMapper.updateById(strategy);
        }
    }

    /**
     * 投票（社区模块）
     *
     * 作用：用户对攻略进行点赞(UP)或点踩(DOWN)操作
     * 业务规则：同一用户对同一攻略只能投一票，重复投票抛出异常
     * 事务：同时更新投票记录和攻略计数
     *
     * @param strategyId 攻略ID
     * @param userId     用户ID
     * @param voteType   投票类型（UP/DOWN）
     * @throws IllegalStateException 已投票时抛出
     */
    @Override
    @Transactional
    public void vote(Long strategyId, Long userId, String voteType) {
        log.info("Vote: strategyId={}, userId={}, voteType={}", strategyId, userId, voteType);

        // 检查是否已投票
        LambdaQueryWrapper<Vote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Vote::getStrategyId, strategyId)
                .eq(Vote::getUserId, userId);

        Vote existingVote = voteMapper.selectOne(wrapper);
        if (existingVote != null) {
            throw new IllegalStateException("已经投过票了");
        }

        Strategy strategy = strategyMapper.selectById(strategyId);
        if (strategy == null) {
            return;
        }

        // 创建投票记录
        Vote vote = new Vote();
        vote.setStrategyId(strategyId);
        vote.setUserId(userId);
        vote.setVoteType(voteType);
        voteMapper.insert(vote);

        // 更新攻略计数
        if ("UP".equalsIgnoreCase(voteType)) {
            strategy.setUpvotes(strategy.getUpvotes() + 1);
        } else if ("DOWN".equalsIgnoreCase(voteType)) {
            strategy.setDownvotes(strategy.getDownvotes() + 1);
        }
        strategyMapper.updateById(strategy);
    }

    /**
     * 取消投票（社区模块）
     *
     * 作用：取消用户对攻略的投票，恢复初始状态
     * 事务：同时删除投票记录和更新攻略计数
     *
     * @param strategyId 攻略ID
     * @param userId     用户ID
     */
    @Override
    @Transactional
    public void cancelVote(Long strategyId, Long userId) {
        LambdaQueryWrapper<Vote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Vote::getStrategyId, strategyId)
                .eq(Vote::getUserId, userId);

        Vote vote = voteMapper.selectOne(wrapper);
        if (vote == null) {
            return;
        }

        Strategy strategy = strategyMapper.selectById(strategyId);
        if (strategy != null) {
            // 恢复计数（确保不小于0）
            if ("UP".equalsIgnoreCase(vote.getVoteType())) {
                strategy.setUpvotes(Math.max(0, strategy.getUpvotes() - 1));
            } else if ("DOWN".equalsIgnoreCase(vote.getVoteType())) {
                strategy.setDownvotes(Math.max(0, strategy.getDownvotes() - 1));
            }
            strategyMapper.updateById(strategy);
        }

        // 删除投票记录
        voteMapper.delete(wrapper);
    }

    /**
     * 删除攻略（社区模块）
     *
     * 作用：删除攻略主体及关联数据（符文关联、装备关联、投票记录）
     * 权限校验：仅攻略作者可删除
     * 事务：确保关联数据一并清除
     *
     * @param strategyId 攻略ID
     * @param userId     当前用户ID（用于权限校验）
     * @throws IllegalArgumentException 攻略不存在或无权删除时抛出
     */
    @Override
    @Transactional
    public void deleteStrategy(Long strategyId, Long userId) {
        log.info("Deleting strategy: strategyId={}, userId={}", strategyId, userId);

        Strategy strategy = strategyMapper.selectById(strategyId);
        if (strategy == null) {
            throw new IllegalArgumentException("攻略不存在");
        }
        if (!strategy.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除他人的攻略");
        }

        strategyAugmentMapper.delete(new LambdaQueryWrapper<StrategyAugment>()
                .eq(StrategyAugment::getStrategyId, strategyId));
        strategyItemMapper.delete(new LambdaQueryWrapper<StrategyItem>()
                .eq(StrategyItem::getStrategyId, strategyId));
        voteMapper.delete(new LambdaQueryWrapper<Vote>()
                .eq(Vote::getStrategyId, strategyId));
        strategyMapper.deleteById(strategyId);

        log.info("Strategy deleted: strategyId={}", strategyId);
    }

    private StrategyListVO convertToListVO(Strategy strategy) {
        StrategyListVO vo = new StrategyListVO();
        vo.setId(strategy.getId());
        vo.setUserId(strategy.getUserId());
        vo.setHeroId(strategy.getHeroId());
        vo.setTitle(strategy.getTitle());
        vo.setDescription(strategy.getDescription());
        vo.setUpvotes(strategy.getUpvotes());
        vo.setDownvotes(strategy.getDownvotes());
        vo.setScore(strategy.getUpvotes() - strategy.getDownvotes());
        vo.setCreatedAt(strategy.getCreatedAt());

        Hero hero = heroMapper.selectById(strategy.getHeroId());
        if (hero != null) {
            vo.setHeroName(hero.getNameZh());
            vo.setHeroIcon(hero.getImageUrl());
        }

        User user = userMapper.selectById(strategy.getUserId());
        if (user != null) {
            vo.setAuthorNickname(user.getNickname());
            vo.setAuthorAvatar(user.getAvatarUrl());
        }

        return vo;
    }

    private StrategyDetailVO convertToDetailVO(Strategy strategy) {
        StrategyDetailVO vo = new StrategyDetailVO();
        vo.setId(strategy.getId());
        vo.setUserId(strategy.getUserId());
        vo.setHeroId(strategy.getHeroId());
        vo.setTitle(strategy.getTitle());
        vo.setDescription(strategy.getDescription());
        vo.setUpvotes(strategy.getUpvotes());
        vo.setDownvotes(strategy.getDownvotes());
        vo.setCreatedAt(strategy.getCreatedAt());

        Hero hero = heroMapper.selectById(strategy.getHeroId());
        if (hero != null) {
            vo.setHeroName(hero.getNameZh());
            vo.setHeroIcon(hero.getImageUrl());
        }

        User user = userMapper.selectById(strategy.getUserId());
        if (user != null) {
            vo.setAuthorNickname(user.getNickname());
            vo.setAuthorAvatar(user.getAvatarUrl());
        }

        return vo;
    }
}