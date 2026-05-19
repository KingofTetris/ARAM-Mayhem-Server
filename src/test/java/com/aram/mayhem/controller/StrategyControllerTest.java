package com.aram.mayhem.controller;

import com.aram.mayhem.common.GlobalExceptionHandler;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.dto.StrategyDetailVO;
import com.aram.mayhem.dto.StrategyListVO;
import com.aram.mayhem.security.JwtAuthenticationFilter;
import com.aram.mayhem.service.StrategyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("StrategyController 测试")
@WebMvcTest(StrategyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StrategyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StrategyService strategyService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private List<StrategyListVO> mockStrategyList;
    private StrategyDetailVO mockStrategyDetail;

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
        StrategyListVO strategy1 = new StrategyListVO();
        strategy1.setId(1L);
        strategy1.setUserId(1L);
        strategy1.setAuthorNickname("玩家一");
        strategy1.setHeroId(1L);
        strategy1.setHeroName("亚索");
        strategy1.setHeroIcon("https://example.com/yasuo.png");
        strategy1.setTitle("亚索核心符文配置");
        strategy1.setDescription("亚索最强符文搭配，胜率超高");
        strategy1.setUpvotes(100);
        strategy1.setDownvotes(10);
        strategy1.setScore(90);
        strategy1.setAugmentIcons(Arrays.asList("icon1.png", "icon2.png"));

        StrategyListVO strategy2 = new StrategyListVO();
        strategy2.setId(2L);
        strategy2.setUserId(2L);
        strategy2.setAuthorNickname("玩家二");
        strategy2.setHeroId(2L);
        strategy2.setHeroName("艾希");
        strategy2.setHeroIcon("https://example.com/ashe.png");
        strategy2.setTitle("艾希发育玩法");
        strategy2.setDescription("艾希前期发育后期输出");
        strategy2.setUpvotes(85);
        strategy2.setDownvotes(15);
        strategy2.setScore(70);

        mockStrategyList = Arrays.asList(strategy1, strategy2);

        mockStrategyDetail = new StrategyDetailVO();
        mockStrategyDetail.setId(1L);
        mockStrategyDetail.setUserId(1L);
        mockStrategyDetail.setAuthorNickname("玩家一");
        mockStrategyDetail.setHeroId(1L);
        mockStrategyDetail.setHeroName("亚索");
        mockStrategyDetail.setHeroIcon("https://example.com/yasuo.png");
        mockStrategyDetail.setTitle("亚索核心符文配置");
        mockStrategyDetail.setDescription("亚索最强符文搭配，胜率超高\n详细描述内容...");
        mockStrategyDetail.setUpvotes(100);
        mockStrategyDetail.setDownvotes(10);
    }

    @Test
    @DisplayName("GET /api/strategies - 获取玩法列表（hot 排序）")
    void getStrategyList_hot_success() throws Exception {
        when(strategyService.getStrategyList(eq("hot"), eq(1), eq(10)))
                .thenReturn(mockStrategyList);

        mockMvc.perform(get("/api/strategies")
                        .param("sort", "hot")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records[0].id").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("亚索核心符文配置"));
    }

    @Test
    @DisplayName("GET /api/strategies - 获取玩法列表（latest 排序）")
    void getStrategyList_latest_success() throws Exception {
        when(strategyService.getStrategyList(eq("latest"), eq(1), eq(10)))
                .thenReturn(mockStrategyList);

        mockMvc.perform(get("/api/strategies")
                        .param("sort", "latest")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("GET /api/strategies/{id} - 获取玩法详情")
    void getStrategyDetail_success() throws Exception {
        when(strategyService.getStrategyDetail(eq(1L))).thenReturn(mockStrategyDetail);

        mockMvc.perform(get("/api/strategies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("亚索核心符文配置"))
                .andExpect(jsonPath("$.data.authorNickname").value("玩家一"));
    }

    @Test
    @DisplayName("GET /api/strategies/{id} - 玩法不存在返回业务错误码404")
    void getStrategyDetail_notFound() throws Exception {
        when(strategyService.getStrategyDetail(eq(999L))).thenReturn(null);

        mockMvc.perform(get("/api/strategies/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("玩法不存在"));
    }

    @Test
    @DisplayName("POST /api/strategies - 未登录返回业务错误码401")
    void createStrategy_unauthorized() throws Exception {
        clearAuth();
        mockMvc.perform(post("/api/strategies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"heroId\": 1, \"title\": \"测试标题\", \"description\": \"这是一段测试描述内容\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("POST /api/strategies - 发布玩法成功")
    void createStrategy_success() throws Exception {
        setupAuth(1L);
        when(strategyService.createStrategy(eq(1L), eq(1L), eq("亚索新玩法"),
                eq("亚索新玩法的详细描述内容"), any(), any()))
                .thenReturn(mockStrategyDetail);

        String requestJson = "{\"heroId\": 1, \"title\": \"亚索新玩法\", \"description\": \"亚索新玩法的详细描述内容\", \"augmentIds\": [1, 2], \"itemIds\": []}";

        mockMvc.perform(post("/api/strategies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
        clearAuth();
    }

    @Test
    @DisplayName("POST /api/strategies - 英雄不存在返回业务错误码400")
    void createStrategy_heroNotFound() throws Exception {
        setupAuth(1L);
        when(strategyService.createStrategy(anyLong(), anyLong(), anyString(), anyString(), any(), any()))
                .thenThrow(new IllegalArgumentException("英雄不存在"));

        String requestJson = "{\"heroId\": 999, \"title\": \"测试标题\", \"description\": \"这是一段测试描述内容\"}";

        mockMvc.perform(post("/api/strategies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("英雄不存在"));
        clearAuth();
    }
}
