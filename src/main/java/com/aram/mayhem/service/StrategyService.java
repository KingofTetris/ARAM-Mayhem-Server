package com.aram.mayhem.service;

import com.aram.mayhem.dto.StrategyDetailVO;
import com.aram.mayhem.dto.StrategyListVO;

import java.util.List;

public interface StrategyService {
    List<StrategyListVO> getStrategyList(String sort, int page, int size);
    StrategyDetailVO getStrategyDetail(Long id);
    StrategyDetailVO createStrategy(Long userId, Long heroId, String title, String description,
                                     List<Long> augmentIds, List<Long> itemIds);
    List<StrategyListVO> getUserStrategies(Long userId);
    void upvoteStrategy(Long strategyId);
    void downvoteStrategy(Long strategyId);
    void vote(Long strategyId, Long userId, String voteType);
    void cancelVote(Long strategyId, Long userId);
}