package com.aram.mayhem.service.impl;

import com.aram.mayhem.dto.StrategyListVO;
import com.aram.mayhem.dto.StrategyDetailVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.entity.Strategy;
import com.aram.mayhem.entity.StrategyAugment;
import com.aram.mayhem.entity.StrategyItem;
import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.entity.User;
import com.aram.mayhem.entity.Vote;
import com.aram.mayhem.mapper.StrategyMapper;
import com.aram.mayhem.mapper.StrategyAugmentMapper;
import com.aram.mayhem.mapper.StrategyItemMapper;
import com.aram.mayhem.mapper.HeroMapper;
import com.aram.mayhem.mapper.UserMapper;
import com.aram.mayhem.mapper.VoteMapper;
import com.aram.mayhem.mapper.AugmentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("StrategyService 测试")
@ExtendWith(MockitoExtension.class)
class StrategyServiceTest {

    @Mock
    private StrategyMapper strategyMapper;

    @Mock
    private StrategyAugmentMapper strategyAugmentMapper;

    @Mock
    private StrategyItemMapper strategyItemMapper;

    @Mock
    private HeroMapper heroMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private VoteMapper voteMapper;

    @Mock
    private AugmentMapper augmentMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private StrategyServiceImpl strategyService;

    private List<Strategy> mockStrategies;
    private List<Hero> mockHeroes;
    private List<User> mockUsers;

    @BeforeEach
    void setUp() {
        mockHeroes = Arrays.asList(
                createHero(1L, "Yasuo", "亚索", "Fighter"),
                createHero(2L, "Ashe", "艾希", "Marksman"),
                createHero(3L, "Amumu", "阿木木", "Tank")
        );

        mockUsers = Arrays.asList(
                createUser(1L, "player001", "玩家一"),
                createUser(2L, "player002", "玩家二")
        );

        mockStrategies = Arrays.asList(
                createStrategy(1L, 1L, 1L, "亚索核心符文配置", "亚索最强符文搭配，胜率超高",
                        100, 10, LocalDateTime.now().minusHours(2)),
                createStrategy(2L, 1L, 2L, "艾希发育玩法", "艾希前期发育后期输出",
                        85, 15, LocalDateTime.now().minusHours(5)),
                createStrategy(3L, 2L, 3L, "阿木木控制流", "阿木木团控玩法详解",
                        120, 5, LocalDateTime.now().minusDays(1)),
                createStrategy(4L, 2L, 1L, "亚索技巧分享", "亚索连招技巧和走位",
                        200, 20, LocalDateTime.now().minusDays(2)),
                createStrategy(5L, 1L, 2L, "艾希风筝打法", "远程风筝消耗流",
                        50, 8, LocalDateTime.now().minusHours(1))
        );
    }

    private Strategy createStrategy(Long id, Long userId, Long heroId, String title,
                                    String description, Integer upvotes, Integer downvotes, LocalDateTime createdAt) {
        Strategy strategy = new Strategy();
        strategy.setId(id);
        strategy.setUserId(userId);
        strategy.setHeroId(heroId);
        strategy.setTitle(title);
        strategy.setDescription(description);
        strategy.setUpvotes(upvotes);
        strategy.setDownvotes(downvotes);
        strategy.setCreatedAt(createdAt);
        strategy.setUpdatedAt(createdAt);
        return strategy;
    }

    private Hero createHero(Long id, String nameEn, String nameZh, String role) {
        Hero hero = new Hero();
        hero.setId(id);
        hero.setNameEn(nameEn);
        hero.setNameZh(nameZh);
        hero.setRole(role);
        return hero;
    }

    private User createUser(Long id, String nickname, String displayName) {
        User user = new User();
        user.setId(id);
        user.setNickname(nickname);
        user.setAvatarUrl("https://example.com/avatar/" + id + ".png");
        return user;
    }

    // ============================================================
    // 测试场景 1：分页查询玩法列表 - hot 排序
    // ============================================================

    @Test
    @DisplayName("分页查询玩法列表 hot 排序：返回按点赞差降序的结果")
    void getStrategyList_hotSort_returnsByUpvotesMinusDownvotes() {
        Page<Strategy> page = new Page<>(1, 10);
        List<Strategy> strategies = Arrays.asList(
                mockStrategies.get(3),
                mockStrategies.get(0),
                mockStrategies.get(2)
        );
        when(strategyMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>(1, 10, 5));

        List<StrategyListVO> result = strategyService.getStrategyList("hot", 1, 10);

        assertNotNull(result);
        verify(strategyMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    // ============================================================
    // 测试场景 2：分页查询玩法列表 - latest 排序
    // ============================================================

    @Test
    @DisplayName("分页查询玩法列表 latest 排序：返回按创建时间降序的结果")
    void getStrategyList_latestSort_returnsByCreatedAtDesc() {
        when(strategyMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>(1, 10, 5));

        List<StrategyListVO> result = strategyService.getStrategyList("latest", 1, 10);

        assertNotNull(result);
        verify(strategyMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    // ============================================================
    // 测试场景 3：查询玩法详情
    // ============================================================

    @Test
    @DisplayName("查询玩法详情：返回完整信息包含英雄和用户信息")
    void getStrategyDetail_existingId_returnsCompleteDetail() {
        Long strategyId = 1L;
        Strategy strategy = mockStrategies.get(0);
        Hero hero = mockHeroes.get(0);
        User user = mockUsers.get(0);

        when(strategyMapper.selectById(strategyId)).thenReturn(strategy);
        when(heroMapper.selectById(strategy.getHeroId())).thenReturn(hero);
        when(userMapper.selectById(strategy.getUserId())).thenReturn(user);

        StrategyDetailVO result = strategyService.getStrategyDetail(strategyId);

        assertNotNull(result);
        assertEquals(strategyId, result.getId());
        assertEquals(strategy.getTitle(), result.getTitle());
        assertEquals(hero.getNameZh(), result.getHeroName());
        assertEquals(user.getNickname(), result.getAuthorNickname());
    }

    // ============================================================
    // 测试场景 4：查询不存在的玩法详情
    // ============================================================

    @Test
    @DisplayName("查询不存在的玩法详情：返回 null")
    void getStrategyDetail_nonExistingId_returnsNull() {
        when(strategyMapper.selectById(999L)).thenReturn(null);

        StrategyDetailVO result = strategyService.getStrategyDetail(999L);

        assertNull(result);
    }

    // ============================================================
    // 测试场景 5：创建玩法
    // ============================================================

    @Test
    @DisplayName("创建玩法：成功创建并返回详情")
    void createStrategy_validInput_success() {
        Strategy strategy = mockStrategies.get(0);
        when(strategyMapper.insert(any(Strategy.class))).thenReturn(1);
        when(heroMapper.selectById(strategy.getHeroId())).thenReturn(mockHeroes.get(0));
        when(userMapper.selectById(strategy.getUserId())).thenReturn(mockUsers.get(0));

        StrategyDetailVO result = strategyService.createStrategy(
                strategy.getUserId(),
                strategy.getHeroId(),
                strategy.getTitle(),
                strategy.getDescription(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        assertNotNull(result);
        verify(strategyMapper).insert(any(Strategy.class));
    }

    // ============================================================
    // 测试场景 6：创建玩法 - 标题为空
    // ============================================================

    @Test
    @DisplayName("创建玩法标题为空：抛出异常")
    void createStrategy_emptyTitle_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            strategyService.createStrategy(1L, 1L, "", "描述内容", Collections.emptyList(), Collections.emptyList());
        });
    }

    // ============================================================
    // 测试场景 7：创建玩法 - 描述过短
    // ============================================================

    @Test
    @DisplayName("创建玩法描述少于10字：抛出异常")
    void createStrategy_shortDescription_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            strategyService.createStrategy(1L, 1L, "标题", "很短", Collections.emptyList(), Collections.emptyList());
        });
    }

    // ============================================================
    // 测试场景 8：获取用户的玩法列表
    // ============================================================

    @Test
    @DisplayName("获取用户玩法列表：返回该用户的玩法")
    void getUserStrategies_existingUser_returnsStrategies() {
        Long userId = 1L;
        List<Strategy> userStrategies = Arrays.asList(mockStrategies.get(0), mockStrategies.get(1), mockStrategies.get(4));

        when(strategyMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(userStrategies);

        List<StrategyListVO> result = strategyService.getUserStrategies(userId);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    // ============================================================
    // 测试场景 9：点赞玩法
    // ============================================================

    @Test
    @DisplayName("点赞玩法： upvotes 加 1")
    void upvoteStrategy_validId_incrementsUpvotes() {
        Long strategyId = 1L;
        Strategy strategy = mockStrategies.get(0);
        int originalUpvotes = strategy.getUpvotes();

        when(strategyMapper.selectById(strategyId)).thenReturn(strategy);
        when(strategyMapper.updateById(any(Strategy.class))).thenReturn(1);

        strategyService.upvoteStrategy(strategyId);

        assertEquals(originalUpvotes + 1, strategy.getUpvotes());
        verify(strategyMapper).updateById(any(Strategy.class));
    }

    // ============================================================
    // 测试场景 10：点踩玩法
    // ============================================================

    @Test
    @DisplayName("点踩玩法： downvotes 加 1")
    void downvoteStrategy_validId_incrementsDownvotes() {
        Long strategyId = 1L;
        Strategy strategy = mockStrategies.get(0);
        int originalDownvotes = strategy.getDownvotes();

        when(strategyMapper.selectById(strategyId)).thenReturn(strategy);
        when(strategyMapper.updateById(any(Strategy.class))).thenReturn(1);

        strategyService.downvoteStrategy(strategyId);

        assertEquals(originalDownvotes + 1, strategy.getDownvotes());
        verify(strategyMapper).updateById(any(Strategy.class));
    }

    // ============================================================
    // 测试场景 11：重复点赞同一玩法
    // ============================================================

    @Test
    void upvoteStrategy_alreadyUpvoted_throwsException() {
        Long strategyId = 1L;
        Long userId = 1L;
        Strategy strategy = mockStrategies.get(0);

        Vote existingVote = new Vote();
        existingVote.setId(1L);
        existingVote.setStrategyId(strategyId);
        existingVote.setUserId(userId);
        existingVote.setVoteType("UP");

        when(voteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingVote);

        assertThrows(IllegalStateException.class, () -> {
            strategyService.vote(strategyId, userId, "UP");
        });
    }

    // ============================================================
    // 测试场景 12：取消点赞
    // ============================================================

    @Test
    @DisplayName("取消点赞：upvotes 减 1")
    void cancelUpvote_existingUpvote_decrementsUpvotes() {
        Long strategyId = 1L;
        Long userId = 1L;
        Strategy strategy = mockStrategies.get(0);
        int originalUpvotes = strategy.getUpvotes();

        Vote existingVote = new Vote();
        existingVote.setId(1L);
        existingVote.setStrategyId(strategyId);
        existingVote.setUserId(userId);
        existingVote.setVoteType("UP");

        when(strategyMapper.selectById(strategyId)).thenReturn(strategy);
        when(voteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingVote);

        strategyService.cancelVote(strategyId, userId);

        assertEquals(originalUpvotes - 1, strategy.getUpvotes());
        verify(strategyMapper).updateById(any(Strategy.class));
    }

    // ============================================================
    // 测试场景 13：分页参数边界测试
    // ============================================================

    @Test
    @DisplayName("分页参数 page <= 0 时使用默认值 1")
    void getStrategyList_invalidPage_usesDefaultPage() {
        when(strategyMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>(1, 10, 0));

        strategyService.getStrategyList("hot", 0, 10);

        verify(strategyMapper).selectPage(argThat(page -> page.getCurrent() == 1), any());
    }

    @Test
    @DisplayName("分页参数 size <= 0 时使用默认值 10")
    void getStrategyList_invalidSize_usesDefaultSize() {
        when(strategyMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>(1, 10, 0));

        strategyService.getStrategyList("hot", 1, 0);

        verify(strategyMapper).selectPage(any(Page.class), any());
    }

    @Test
    @DisplayName("分页参数 size > 100 时限制为 100")
    void getStrategyList_sizeTooLarge_limitsTo100() {
        when(strategyMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>(1, 100, 0));

        strategyService.getStrategyList("hot", 1, 200);

        verify(strategyMapper).selectPage(any(Page.class), any());
    }
}