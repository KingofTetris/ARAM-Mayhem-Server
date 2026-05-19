package com.aram.mayhem.controller;

import com.aram.mayhem.common.BusinessException;
import com.aram.mayhem.common.GlobalExceptionHandler;
import com.aram.mayhem.dto.UpdateProfileRequest;
import com.aram.mayhem.dto.UserProfileVO;
import com.aram.mayhem.security.JwtAuthenticationFilter;
import com.aram.mayhem.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("UserController 测试")
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private UserProfileVO mockProfile;

    private void setupAuth(Long userId) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId.toString(), null, java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void setUp() {
        clearAuth();
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
        @DisplayName("未认证用户 - SecurityContext为空时Controller抛NPE由全局异常处理")
        void getUserProfile_unauthorized() throws Exception {
            clearAuth();
            mockMvc.perform(get("/api/users/me/profile"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("认证用户获取资料成功")
        void getUserProfile_success() throws Exception {
            setupAuth(1L);
            when(userService.getUserProfile(1L)).thenReturn(mockProfile);

            mockMvc.perform(get("/api/users/me/profile"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.nickname").value("测试用户"))
                    .andExpect(jsonPath("$.data.avatarUrl").value("https://example.com/avatar.png"))
                    .andExpect(jsonPath("$.data.displayMode").value(0))
                    .andExpect(jsonPath("$.data.notificationEnabled").value(1))
                    .andExpect(jsonPath("$.data.role").value("USER"))
                    .andExpect(jsonPath("$.data.strategyCount").value(5))
                    .andExpect(jsonPath("$.data.favoriteCount").value(0));
            clearAuth();
        }

        @Test
        @DisplayName("用户不存在返回业务错误")
        void getUserProfile_userNotFound() throws Exception {
            setupAuth(999L);
            when(userService.getUserProfile(999L))
                    .thenThrow(new BusinessException(404, "User not found with id: 999"));

            mockMvc.perform(get("/api/users/me/profile"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(404));
            clearAuth();
        }
    }

    @Nested
    @DisplayName("PATCH /api/users/me 测试")
    class UpdateProfileTest {

        @Test
        @DisplayName("未认证用户 - SecurityContext为空时Controller抛NPE由全局异常处理")
        void updateProfile_unauthorized() throws Exception {
            clearAuth();
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .nickname("新昵称")
                    .build();

            mockMvc.perform(patch("/api/users/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("认证用户更新昵称成功")
        void updateProfile_nickname() throws Exception {
            setupAuth(1L);
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
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.nickname").value("新昵称"));
            clearAuth();
        }

        @Test
        @DisplayName("认证用户更新显示模式成功")
        void updateProfile_displayMode() throws Exception {
            setupAuth(1L);
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
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.displayMode").value(1));
            clearAuth();
        }

        @Test
        @DisplayName("昵称超长校验失败")
        void updateProfile_nicknameTooLong() throws Exception {
            setupAuth(1L);
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .nickname("a".repeat(31))
                    .build();

            mockMvc.perform(patch("/api/users/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
            clearAuth();
        }

        @Test
        @DisplayName("用户不存在返回业务错误")
        void updateProfile_userNotFound() throws Exception {
            setupAuth(999L);
            when(userService.updateProfile(eq(999L), any(UpdateProfileRequest.class)))
                    .thenThrow(new BusinessException(404, "User not found with id: 999"));

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .nickname("新昵称")
                    .build();

            mockMvc.perform(patch("/api/users/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(404));
            clearAuth();
        }
    }
}
