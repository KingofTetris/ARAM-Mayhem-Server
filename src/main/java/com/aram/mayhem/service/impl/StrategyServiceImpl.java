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

    @Override
    public List<StrategyListVO> getStrategyList(String sort, int page, int size) {
        if (page <= 0) page = 1;
        if (size <= 0) size = 10;
        if (size > 100) size = 100;

        Page<Strategy> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Strategy> wrapper = new LambdaQueryWrapper<>();

        if ("hot".equalsIgnoreCase(sort)) {
            wrapper.orderByDesc(s -> s.getUpvotes() - s.getDownvotes());
        } else {
            wrapper.orderByDesc(Strategy::getCreatedAt);
        }

        Page<Strategy> result = strategyMapper.selectPage(pageParam, wrapper);

        return result.getRecords().stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());
    }

    @Override
    public StrategyDetailVO getStrategyDetail(Long id) {
        Strategy strategy = strategyMapper.selectById(id);
        if (strategy == null) {
            return null;
        }
        return convertToDetailVO(strategy);
    }

    @Override
    @Transactional
    public StrategyDetailVO createStrategy(Long userId, Long heroId, String title, String description,
                                           List<Long> augmentIds, List<Long> itemIds) {
        log.info("Creating strategy: userId={}, heroId={}, title={}", userId, heroId, title);

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (description == null || description.trim().length() < 10) {
            throw new IllegalArgumentException("描述至少需要10个字");
        }

        Strategy strategy = new Strategy();
        strategy.setUserId(userId);
        strategy.setHeroId(heroId);
        strategy.setTitle(title.trim());
        strategy.setDescription(description.trim());
        strategy.setUpvotes(0);
        strategy.setDownvotes(0);

        strategyMapper.insert(strategy);
        log.info("Strategy created: strategyId={}, userId={}", strategy.getId(), userId);

        if (augmentIds != null && !augmentIds.isEmpty()) {
            for (Long augmentId : augmentIds) {
                StrategyAugment sa = new StrategyAugment();
                sa.setStrategyId(strategy.getId());
                sa.setAugmentId(augmentId);
                strategyAugmentMapper.insert(sa);
            }
        }

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

    @Override
    public List<StrategyListVO> getUserStrategies(Long userId) {
        LambdaQueryWrapper<Strategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Strategy::getUserId, userId)
                .orderByDesc(Strategy::getCreatedAt);

        return strategyMapper.selectList(wrapper).stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void upvoteStrategy(Long strategyId) {
        Strategy strategy = strategyMapper.selectById(strategyId);
        if (strategy != null) {
            strategy.setUpvotes(strategy.getUpvotes() + 1);
            strategyMapper.updateById(strategy);
        }
    }

    @Override
    @Transactional
    public void downvoteStrategy(Long strategyId) {
        Strategy strategy = strategyMapper.selectById(strategyId);
        if (strategy != null) {
            strategy.setDownvotes(strategy.getDownvotes() + 1);
            strategyMapper.updateById(strategy);
        }
    }

    @Override
    @Transactional
    public void vote(Long strategyId, Long userId, String voteType) {
        log.info("Vote: strategyId={}, userId={}, voteType={}", strategyId, userId, voteType);

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

        Vote vote = new Vote();
        vote.setStrategyId(strategyId);
        vote.setUserId(userId);
        vote.setVoteType(voteType);
        voteMapper.insert(vote);

        if ("UP".equalsIgnoreCase(voteType)) {
            strategy.setUpvotes(strategy.getUpvotes() + 1);
        } else if ("DOWN".equalsIgnoreCase(voteType)) {
            strategy.setDownvotes(strategy.getDownvotes() + 1);
        }
        strategyMapper.updateById(strategy);
    }

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
            if ("UP".equalsIgnoreCase(vote.getVoteType())) {
                strategy.setUpvotes(Math.max(0, strategy.getUpvotes() - 1));
            } else if ("DOWN".equalsIgnoreCase(vote.getVoteType())) {
                strategy.setDownvotes(Math.max(0, strategy.getDownvotes() - 1));
            }
            strategyMapper.updateById(strategy);
        }

        voteMapper.delete(wrapper);
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