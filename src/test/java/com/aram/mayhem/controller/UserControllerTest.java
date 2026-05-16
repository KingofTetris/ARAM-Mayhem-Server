package com.aram.mayhem.controller;

import com.aram.mayhem.common.BusinessException;
import com.aram.mayhem.dto.UpdateProfileRequest;
import com.aram.mayhem.dto.UserProfileVO;
import com.aram.mayhem.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController 测试
 *
 * 覆盖范围：GET /api/users/me/profile、PATCH /api/users/me
 * 测试策略：Mock UserService，验证 HTTP 响应和权限控制
 */
@DisplayName("UserController 测试")
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserProfileVO mockProfile;

    @BeforeEach
    void setUp() {
        mockProfile = UserProfileVO.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("测试用户")
                .avatarUrl("https://example.com/avatar.png")
                .displayMode(0)
                .notificationEnabled(1)
                .role("USER")
                .strategyCount(5)
                .favoriteCount(0)
                .build();
    }

    @Nested
    @DisplayName("GET /api/users/me/profile 测试")
    class GetUserProfileTest {

        @Test
        @DisplayName("未认证用户返回401")
        void getUserProfile_unauthorized() throws Exception {
            mockMvc.perform(get("/api/users/me/profile"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("认证用户获取资料成功")
        @WithMockUser(username = "1")
        void getUserProfile_success() throws Exception {
            when(userService.getUserProfile(1L)).thenReturn(mockProfile);

            mockMvc.perform(get("/api/users/me/profile"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.nickname").value("测试用户"))
                    .andExpect(jsonPath("$.data.avatarUrl").value("https://example.com/avatar.png"))
                    .andExpect(jsonPath("$.data.displayMode").value(0))
                    .andExpect(jsonPath("$.data.notificationEnabled").value(1))
                    .andExpect(jsonPath("$.data.role").value("USER"))
                    .andExpect(jsonPath("$.data.strategyCount").value(5))
                    .andExpect(jsonPath("$.data.favoriteCount").value(0));
        }

        @Test
        @DisplayName("用户不存在返回错误")
        @WithMockUser(username = "999")
        void getUserProfile_userNotFound() throws Exception {
            when(userService.getUserProfile(999L))
                    .thenThrow(new BusinessException(404, "User not found with id: 999"));

            mockMvc.perform(get("/api/users/me/profile"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/users/me 测试")
    class UpdateProfileTest {

        @Test
        @DisplayName("未认证用户返回401")
        void updateProfile_unauthorized() throws Exception {
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .nickname("新昵称")
                    .build();

            mockMvc.perform(patch("/api/users/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("认证用户更新昵称成功")
        @WithMockUser(username = "1")
        void updateProfile_nickname() throws Exception {
            UserProfileVO updated = UserProfileVO.builder()
                    .id(1L)
                    .email("test@example.com")
                    .nickname("新昵称")
                    .avatarUrl("https://example.com/avatar.png")
                    .displayMode(0)
                    .notificationEnabled(1)
                    .role("USER")
                    .strategyCount(5)
                    .favoriteCount(0)
                    .build();

            when(userService.updateProfile(eq(1L), any(UpdateProfileRequest.class))).thenReturn(updated);

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .nickname("新昵称")
                    .build();

            mockMvc.perform(patch("/api/users/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.nickname").value("新昵称"));
        }

        @Test
        @DisplayName("认证用户更新显示模式成功")
        @WithMockUser(username = "1")
        void updateProfile_displayMode() throws Exception {
            UserProfileVO updated = UserProfileVO.builder()
                    .id(1L)
                    .email("test@example.com")
                    .nickname("测试用户")
                    .avatarUrl("https://example.com/avatar.png")
                    .displayMode(1)
                    .notificationEnabled(1)
                    .role("USER")
                    .strategyCount(5)
                    .favoriteCount(0)
                    .build();

            when(userService.updateProfile(eq(1L), any(UpdateProfileRequest.class))).thenReturn(updated);

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .displayMode(1)
                    .build();

            mockMvc.perform(patch("/api/users/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.displayMode").value(1));
        }

        @Test
        @DisplayName("昵称超长校验失败")
        @WithMockUser(username = "1")
        void updateProfile_nicknameTooLong() throws Exception {
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .nickname("a".repeat(31))
                    .build();

            mockMvc.perform(patch("/api/users/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("用户不存在返回错误")
        @WithMockUser(username = "999")
        void updateProfile_userNotFound() throws Exception {
            when(userService.updateProfile(eq(999L), any(UpdateProfileRequest.class)))
                    .thenThrow(new BusinessException(404, "User not found with id: 999"));

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .nickname("新昵称")
                    .build();

            mockMvc.perform(patch("/api/users/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }
}
