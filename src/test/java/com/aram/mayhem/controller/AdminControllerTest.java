package com.aram.mayhem.controller;

import com.aram.mayhem.common.GlobalExceptionHandler;
import com.aram.mayhem.dto.SyncResult;
import com.aram.mayhem.dto.TrapMarkRequest;
import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.mapper.AugmentMapper;
import com.aram.mayhem.mapper.HeroMapper;
import com.aram.mayhem.scheduler.DataSyncScheduler;
import com.aram.mayhem.security.JwtAuthenticationFilter;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AdminController 测试")
@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HeroMapper heroMapper;

    @MockBean
    private AugmentMapper augmentMapper;

    @MockBean
    private DataSyncScheduler dataSyncScheduler;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private void setupAdminAuth() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        "1", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void setupUserAuth() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        "2", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("PUT /api/admin/heroes/{id}/trap-mark 测试")
    class MarkHeroTrapTest {

        @Test
        @DisplayName("ADMIN 用户标记英雄版本陷阱成功")
        void markHeroTrap_success() throws Exception {
            setupAdminAuth();
            Hero hero = new Hero();
            hero.setId(1L);
            hero.setNameEn("Aatrox");
            when(heroMapper.selectById(1L)).thenReturn(hero);
            when(heroMapper.updateById(hero)).thenReturn(1);

            TrapMarkRequest request = new TrapMarkRequest();
            request.setIsVersionTrap(true);

            mockMvc.perform(put("/api/admin/heroes/1/trap-mark")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(heroMapper).updateById(hero);
            clearAuth();
        }

        @Test
        @DisplayName("ADMIN 用户取消英雄版本陷阱成功")
        void unmarkHeroTrap_success() throws Exception {
            setupAdminAuth();
            Hero hero = new Hero();
            hero.setId(1L);
            hero.setNameEn("Aatrox");
            hero.setIsVersionTrap(true);
            hero.setVersionTrapSince(LocalDateTime.now());
            when(heroMapper.selectById(1L)).thenReturn(hero);
            when(heroMapper.updateById(hero)).thenReturn(1);

            TrapMarkRequest request = new TrapMarkRequest();
            request.setIsVersionTrap(false);

            mockMvc.perform(put("/api/admin/heroes/1/trap-mark")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            clearAuth();
        }

        @Test
        @DisplayName("英雄不存在返回业务错误码404")
        void markHeroTrap_heroNotFound() throws Exception {
            setupAdminAuth();
            when(heroMapper.selectById(999L)).thenReturn(null);

            TrapMarkRequest request = new TrapMarkRequest();
            request.setIsVersionTrap(true);

            mockMvc.perform(put("/api/admin/heroes/999/trap-mark")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(404));

            clearAuth();
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/augments/{id}/trap-mark 测试")
    class MarkAugmentTrapTest {

        @Test
        @DisplayName("ADMIN 用户标记符文版本陷阱成功")
        void markAugmentTrap_success() throws Exception {
            setupAdminAuth();
            Augment augment = new Augment();
            augment.setId(1L);
            augment.setNameEn("PoroKing");
            when(augmentMapper.selectById(1L)).thenReturn(augment);
            when(augmentMapper.updateById(augment)).thenReturn(1);

            TrapMarkRequest request = new TrapMarkRequest();
            request.setIsVersionTrap(true);

            mockMvc.perform(put("/api/admin/augments/1/trap-mark")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(augmentMapper).updateById(augment);
            clearAuth();
        }

        @Test
        @DisplayName("符文不存在返回业务错误码404")
        void markAugmentTrap_augmentNotFound() throws Exception {
            setupAdminAuth();
            when(augmentMapper.selectById(999L)).thenReturn(null);

            TrapMarkRequest request = new TrapMarkRequest();
            request.setIsVersionTrap(true);

            mockMvc.perform(put("/api/admin/augments/999/trap-mark")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(404));

            clearAuth();
        }
    }

    @Nested
    @DisplayName("POST /api/admin/sync/trigger 测试")
    class TriggerSyncTest {

        @Test
        @DisplayName("ADMIN 用户手动触发同步成功")
        void triggerSync_success() throws Exception {
            setupAdminAuth();
            SyncResult syncResult = SyncResult.success(10, 5, 3, 1, 18, 0, 1500L);
            when(dataSyncScheduler.syncAllData()).thenReturn(syncResult);

            mockMvc.perform(post("/api/admin/sync/trigger")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.success").value(true))
                    .andExpect(jsonPath("$.data.heroInserted").value(10))
                    .andExpect(jsonPath("$.data.heroUpdated").value(5));

            clearAuth();
        }

        @Test
        @DisplayName("同步失败返回错误")
        void triggerSync_failed() throws Exception {
            setupAdminAuth();
            SyncResult syncResult = SyncResult.fail("Another sync is already running");
            when(dataSyncScheduler.syncAllData()).thenReturn(syncResult);

            mockMvc.perform(post("/api/admin/sync/trigger")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));

            clearAuth();
        }
    }
}
