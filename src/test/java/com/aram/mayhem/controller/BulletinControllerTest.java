package com.aram.mayhem.controller;

import com.aram.mayhem.common.GlobalExceptionHandler;
import com.aram.mayhem.dto.BulletinDetailVO;
import com.aram.mayhem.dto.BulletinListVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.service.BulletinService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("BulletinController 测试")
@WebMvcTest(BulletinController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class BulletinControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BulletinService bulletinService;

    @Test
    @DisplayName("GET /api/bulletins - 无筛选返回公告列表")
    void testGetBulletinList_noFilter() throws Exception {
        BulletinListVO vo = createBulletinVO(1L, "version", "v14.10 更新");
        PageResult<BulletinListVO> pageResult = new PageResult<>(1, 1, 10, List.of(vo));

        when(bulletinService.getBulletinList(isNull(), eq(1), eq(10))).thenReturn(pageResult);

        mockMvc.perform(get("/api/bulletins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("v14.10 更新"));
    }

    @Test
    @DisplayName("GET /api/bulletins?type=version - 按类型筛选")
    void testGetBulletinList_withTypeFilter() throws Exception {
        BulletinListVO vo = createBulletinVO(1L, "version", "v14.10 更新");
        PageResult<BulletinListVO> pageResult = new PageResult<>(1, 1, 10, List.of(vo));

        when(bulletinService.getBulletinList(eq("version"), eq(1), eq(10))).thenReturn(pageResult);

        mockMvc.perform(get("/api/bulletins").param("type", "version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].type").value("version"));
    }

    @Test
    @DisplayName("GET /api/bulletins/latest - 获取最新公告")
    void testGetLatestBulletins() throws Exception {
        BulletinListVO vo1 = createBulletinVO(1L, "version", "v14.10 更新");
        BulletinListVO vo2 = createBulletinVO(2L, "event", "五一活动");

        when(bulletinService.getLatestBulletins(3)).thenReturn(List.of(vo1, vo2));

        mockMvc.perform(get("/api/bulletins/latest").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/bulletins/{id} - 获取公告详情")
    void testGetBulletinDetail() throws Exception {
        BulletinDetailVO detail = new BulletinDetailVO();
        detail.setId(1L);
        detail.setType("version");
        detail.setTitle("v14.10 更新");
        detail.setContent("详细内容");
        detail.setCreatedAt(LocalDateTime.now());

        when(bulletinService.getBulletinDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/bulletins/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("v14.10 更新"));
    }

    private BulletinListVO createBulletinVO(Long id, String type, String title) {
        BulletinListVO vo = new BulletinListVO();
        vo.setId(id);
        vo.setType(type);
        vo.setTitle(title);
        vo.setCreatedAt(LocalDateTime.now());
        return vo;
    }
}
