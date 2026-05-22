package com.aram.mayhem.controller;

import com.aram.mayhem.dto.TrapMarkRequest;
import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.mapper.AugmentMapper;
import com.aram.mayhem.mapper.HeroMapper;
import com.aram.mayhem.scheduler.DataSyncScheduler;
import com.aram.mayhem.service.CacheWarmupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AdminController @PreAuthorize 权限校验测试")
class AdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HeroMapper heroMapper;

    @MockBean
    private AugmentMapper augmentMapper;

    @MockBean
    private DataSyncScheduler dataSyncScheduler;

    @MockBean
    private CacheWarmupService cacheWarmupService;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("非 ADMIN 用户访问 Admin 接口被拒绝")
    class NonAdminAccessTest {

        @Test
        @DisplayName("ROLE_USER 用户访问 trap-mark 返回 403")
        @WithMockUser(roles = "USER")
        void shouldRejectUserRole() throws Exception {
            TrapMarkRequest request = new TrapMarkRequest();
            request.setIsVersionTrap(true);

            MvcResult result = mockMvc.perform(put("/api/admin/heroes/1/trap-mark")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isForbidden())
                    .andReturn();
        }

        @Test
        @DisplayName("无角色用户访问 Admin 接口返回 403")
        @WithMockUser(roles = {})
        void shouldRejectNoRoleUser() throws Exception {
            mockMvc.perform(post("/api/admin/sync/trigger")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("ADMIN 用户访问 Admin 接口成功")
    class AdminAccessTest {

        @Test
        @DisplayName("ROLE_ADMIN 用户访问 trap-mark 成功")
        @WithMockUser(roles = "ADMIN")
        void shouldAllowAdminRole() throws Exception {
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
        }
    }
}
