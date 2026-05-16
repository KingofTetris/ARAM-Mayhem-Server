package com.aram.mayhem.service;

import com.aram.mayhem.dto.UpdateProfileRequest;
import com.aram.mayhem.dto.UserProfileVO;

/**
 * 用户服务接口
 *
 * 功能：用户资料查询、用户资料更新
 * 实现：UserServiceImpl
 */
public interface UserService {

    /**
     * 获取用户资料（个人中心模块）
     *
     * 作用：根据用户ID获取完整的个人资料信息，包括攻略数和收藏数
     *
     * @param userId 用户ID
     * @return UserProfileVO 用户资料视图对象
     */
    UserProfileVO getUserProfile(Long userId);

    /**
     * 更新用户资料（个人中心模块）
     *
     * 作用：更新用户的昵称、头像、显示模式、通知开关等个人信息
     * 仅更新非 null 字段（部分更新策略）
     *
     * @param userId  用户ID
     * @param request 更新请求体（仅包含需要修改的字段）
     * @return UserProfileVO 更新后的用户资料
     */
    UserProfileVO updateProfile(Long userId, UpdateProfileRequest request);
}
