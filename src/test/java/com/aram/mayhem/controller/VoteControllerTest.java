package com.aram.mayhem.controller;

import com.aram.mayhem.common.GlobalExceptionHandler;
import com.aram.mayhem.dto.VoteRequest;
import com.aram.mayhem.security.JwtAuthenticationFilter;
import com.aram.mayhem.service.StrategyService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("VoteController 测试")
@WebMvcTest(VoteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class VoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StrategyService strategyService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private void setupAuth(Long userId) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId.toString(), null, java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private VoteRequest createVoteRequest(String voteType) {
        VoteRequest request = new VoteRequest();
        request.setVoteType(voteType);
        return request;
    }

    @Test
    @DisplayName("POST /api/strategies/{id}/vote - 未登录返回401")
    void vote_unauthorized() throws Exception {
        clearAuth();
        VoteRequest request = createVoteRequest("UP");

        mockMvc.perform(post("/api/strategies/1/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("POST /api/strategies/{id}/vote - UP 投票成功")
    void vote_up_success() throws Exception {
        setupAuth(1L);
        doNothing().when(strategyService).vote(eq(1L), eq(1L), eq("UP"));

        VoteRequest request = createVoteRequest("UP");

        mockMvc.perform(post("/api/strategies/1/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(strategyService).vote(1L, 1L, "UP");
        clearAuth();
    }

    @Test
    @DisplayName("POST /api/strategies/{id}/vote - DOWN 投票成功")
    void vote_down_success() throws Exception {
        setupAuth(1L);
        doNothing().when(strategyService).vote(eq(1L), eq(1L), eq("DOWN"));

        VoteRequest request = createVoteRequest("DOWN");

        mockMvc.perform(post("/api/strategies/1/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(strategyService).vote(1L, 1L, "DOWN");
        clearAuth();
    }

    @Test
    @DisplayName("POST /api/strategies/{id}/vote - 玩法不存在抛出异常")
    void vote_strategyNotFound() throws Exception {
        setupAuth(1L);
        doThrow(new IllegalStateException("玩法不存在")).when(strategyService).vote(eq(999L), eq(1L), eq("UP"));

        VoteRequest request = createVoteRequest("UP");

        mockMvc.perform(post("/api/strategies/999/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("玩法不存在"));
        clearAuth();
    }

    @Test
    @DisplayName("POST /api/strategies/{id}/vote - 重复投票抛出异常")
    void vote_alreadyVoted() throws Exception {
        setupAuth(1L);
        doThrow(new IllegalStateException("您已经投过票了")).when(strategyService).vote(eq(1L), eq(1L), eq("UP"));

        VoteRequest request = createVoteRequest("UP");

        mockMvc.perform(post("/api/strategies/1/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("您已经投过票了"));
        clearAuth();
    }

    @Test
    @DisplayName("DELETE /api/strategies/{id}/vote - 取消投票成功")
    void cancelVote_success() throws Exception {
        setupAuth(1L);
        doNothing().when(strategyService).cancelVote(eq(1L), eq(1L));

        mockMvc.perform(delete("/api/strategies/1/vote")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(strategyService).cancelVote(1L, 1L);
        clearAuth();
    }

    @Test
    @DisplayName("DELETE /api/strategies/{id}/vote - 未投票时取消抛出异常返回500")
    void cancelVote_notVoted() throws Exception {
        setupAuth(1L);
        doThrow(new IllegalStateException("您还没有投票")).when(strategyService).cancelVote(eq(1L), eq(1L));

        mockMvc.perform(delete("/api/strategies/1/vote")
                        .with(csrf()))
                .andExpect(status().isInternalServerError());
        clearAuth();
    }

    @Test
    @DisplayName("DELETE /api/strategies/{id}/vote - 未登录无法取消投票")
    void cancelVote_unauthorized() throws Exception {
        clearAuth();
        mockMvc.perform(delete("/api/strategies/1/vote")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}
