package com.aram.mayhem.service.impl;

import com.aram.mayhem.common.BusinessException;
import com.aram.mayhem.dto.UpdateProfileRequest;
import com.aram.mayhem.dto.UserProfileVO;
import com.aram.mayhem.entity.Strategy;
import com.aram.mayhem.entity.User;
import com.aram.mayhem.mapper.StrategyMapper;
import com.aram.mayhem.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * UserServiceImpl 单元测试
 *
 * 覆盖范围：getUserProfile、updateProfile
 * 测试策略：Mock UserMapper 和 StrategyMapper，验证业务逻辑
 */
@DisplayName("UserServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private StrategyMapper strategyMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@example.com");
        mockUser.setNickname("测试用户");
        mockUser.setAvatarUrl("https://example.com/avatar.png");
        mockUser.setDisplayMode(0);
        mockUser.setNotificationEnabled(1);
        mockUser.setRole("USER");
    }

    @Nested
    @DisplayName("getUserProfile 测试")
    class GetUserProfileTest {

        @Test
        @DisplayName("正常获取用户资料")
        void getUserProfile_success() {
            when(userMapper.selectById(1L)).thenReturn(mockUser);
            when(strategyMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

            UserProfileVO result = userService.getUserProfile(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getNickname()).isEqualTo("测试用户");
            assertThat(result.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
            assertThat(result.getDisplayMode()).isEqualTo(0);
            assertThat(result.getNotificationEnabled()).isEqualTo(1);
            assertThat(result.getRole()).isEqualTo("USER");
            assertThat(result.getStrategyCount()).isEqualTo(5);
            assertThat(result.getFavoriteCount()).isEqualTo(0);

            verify(userMapper).selectById(1L);
            verify(strategyMapper).selectCount(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void getUserProfile_userNotFound() {
            when(userMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> userService.getUserProfile(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("User not found");

            verify(userMapper).selectById(999L);
            verify(strategyMapper, never()).selectCount(any());
        }

        @Test
        @DisplayName("攻略数为0时正确返回")
        void getUserProfile_zeroStrategies() {
            when(userMapper.selectById(1L)).thenReturn(mockUser);
            when(strategyMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            UserProfileVO result = userService.getUserProfile(1L);

            assertThat(result.getStrategyCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("updateProfile 测试")
    class UpdateProfileTest {

        @Test
        @DisplayName("更新昵称")
        void updateProfile_nickname() {
            when(userMapper.selectById(1L)).thenReturn(mockUser);
            when(userMapper.updateById(any(User.class))).thenReturn(1);
            when(strategyMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .nickname("新昵称")
                    .build();

            UserProfileVO result = userService.updateProfile(1L, request);

            assertThat(result.getNickname()).isEqualTo("新昵称");
            verify(userMapper).updateById(any(User.class));
        }

        @Test
        @DisplayName("更新头像")
        void updateProfile_avatarUrl() {
            when(userMapper.selectById(1L)).thenReturn(mockUser);
            when(userMapper.updateById(any(User.class))).thenReturn(1);
            when(strategyMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .avatarUrl("https://example.com/new-avatar.png")
                    .build();

            UserProfileVO result = userService.updateProfile(1L, request);

            assertThat(result.getAvatarUrl()).isEqualTo("https://example.com/new-avatar.png");
            verify(userMapper).updateById(any(User.class));
        }

        @Test
        @DisplayName("更新显示模式")
        void updateProfile_displayMode() {
            when(userMapper.selectById(1L)).thenReturn(mockUser);
            when(userMapper.updateById(any(User.class))).thenReturn(1);
            when(strategyMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .displayMode(1)
                    .build();

            UserProfileVO result = userService.updateProfile(1L, request);

            assertThat(result.getDisplayMode()).isEqualTo(1);
            verify(userMapper).updateById(any(User.class));
        }

        @Test
        @DisplayName("更新通知开关")
        void updateProfile_notificationEnabled() {
            when(userMapper.selectById(1L)).thenReturn(mockUser);
            when(userMapper.updateById(any(User.class))).thenReturn(1);
            when(strategyMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .notificationEnabled(0)
                    .build();

            UserProfileVO result = userService.updateProfile(1L, request);

            assertThat(result.getNotificationEnabled()).isEqualTo(0);
            verify(userMapper).updateById(any(User.class));
        }

        @Test
        @DisplayName("部分更新 - null字段不更新")
        void updateProfile_partialUpdate_nullFieldsIgnored() {
            when(userMapper.selectById(1L)).thenReturn(mockUser);
            when(userMapper.updateById(any(User.class))).thenReturn(1);
            when(strategyMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .nickname("新昵称")
                    .build();

            UserProfileVO result = userService.updateProfile(1L, request);

            assertThat(result.getNickname()).isEqualTo("新昵称");
            assertThat(result.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
            assertThat(result.getDisplayMode()).isEqualTo(0);
            assertThat(result.getNotificationEnabled()).isEqualTo(1);
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void updateProfile_userNotFound() {
            when(userMapper.selectById(999L)).thenReturn(null);

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .nickname("新昵称")
                    .build();

            assertThatThrownBy(() -> userService.updateProfile(999L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("User not found");

            verify(userMapper).selectById(999L);
            verify(userMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("更新所有字段")
        void updateProfile_allFields() {
            when(userMapper.selectById(1L)).thenReturn(mockUser);
            when(userMapper.updateById(any(User.class))).thenReturn(1);
            when(strategyMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .nickname("全新昵称")
                    .avatarUrl("https://example.com/all-new.png")
                    .displayMode(1)
                    .notificationEnabled(0)
                    .build();

            UserProfileVO result = userService.updateProfile(1L, request);

            assertThat(result.getNickname()).isEqualTo("全新昵称");
            assertThat(result.getAvatarUrl()).isEqualTo("https://example.com/all-new.png");
            assertThat(result.getDisplayMode()).isEqualTo(1);
            assertThat(result.getNotificationEnabled()).isEqualTo(0);
        }
    }
}
