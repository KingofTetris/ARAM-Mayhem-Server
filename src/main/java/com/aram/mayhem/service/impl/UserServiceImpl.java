package com.aram.mayhem.service.impl;

import com.aram.mayhem.common.BusinessException;
import com.aram.mayhem.dto.UpdateProfileRequest;
import com.aram.mayhem.dto.UserProfileVO;
import com.aram.mayhem.entity.Strategy;
import com.aram.mayhem.entity.User;
import com.aram.mayhem.mapper.StrategyMapper;
import com.aram.mayhem.mapper.UserMapper;
import com.aram.mayhem.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 *
 * 功能：用户资料查询、用户资料更新（部分更新策略）
 * 关联：UserMapper, StrategyMapper, UserService
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;
    private final StrategyMapper strategyMapper;

    public UserServiceImpl(UserMapper userMapper, StrategyMapper strategyMapper) {
        this.userMapper = userMapper;
        this.strategyMapper = strategyMapper;
    }

    /**
     * 获取用户资料（个人中心模块）
     *
     * 作用：根据用户ID获取完整的个人资料信息，包括攻略数统计
     *
     * @param userId 用户ID
     * @return UserProfileVO 用户资料视图对象
     * @throws BusinessException 用户不存在时抛出404错误
     */
    @Override
    public UserProfileVO getUserProfile(Long userId) {
        log.info("Getting user profile: userId={}", userId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("User not found: userId={}", userId);
            throw new BusinessException(404, "User not found with id: " + userId);
        }

        Long strategyCount = strategyMapper.selectCount(
                new LambdaQueryWrapper<Strategy>().eq(Strategy::getUserId, userId)
        );

        return convertToProfileVO(user, strategyCount.intValue());
    }

    /**
     * 更新用户资料（个人中心模块）
     *
     * 作用：部分更新用户信息，仅更新请求中非 null 的字段
     * 可更新字段：nickname、avatarUrl、displayMode、notificationEnabled
     *
     * @param userId  用户ID
     * @param request 更新请求体
     * @return UserProfileVO 更新后的用户资料
     * @throws BusinessException 用户不存在时抛出404错误
     */
    @Override
    public UserProfileVO updateProfile(Long userId, UpdateProfileRequest request) {
        log.info("Updating user profile: userId={}, nickname={}", userId, request.getNickname());

        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("User not found for update: userId={}", userId);
            throw new BusinessException(404, "User not found with id: " + userId);
        }

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getDisplayMode() != null) {
            user.setDisplayMode(request.getDisplayMode());
        }
        if (request.getNotificationEnabled() != null) {
            user.setNotificationEnabled(request.getNotificationEnabled());
        }

        userMapper.updateById(user);
        log.info("User profile updated: userId={}", userId);

        Long strategyCount = strategyMapper.selectCount(
                new LambdaQueryWrapper<Strategy>().eq(Strategy::getUserId, userId)
        );

        return convertToProfileVO(user, strategyCount.intValue());
    }

    /**
     * User 实体 → UserProfileVO 转换（私有方法）
     *
     * @param user          用户实体
     * @param strategyCount 攻略数量
     * @return UserProfileVO 用户资料视图对象
     */
    private UserProfileVO convertToProfileVO(User user, int strategyCount) {
        return UserProfileVO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .displayMode(user.getDisplayMode())
                .notificationEnabled(user.getNotificationEnabled())
                .role(user.getRole())
                .strategyCount(strategyCount)
                .favoriteCount(0)
                .build();
    }
}
