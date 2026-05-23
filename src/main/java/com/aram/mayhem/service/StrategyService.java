package com.aram.mayhem.service;

import com.aram.mayhem.dto.StrategyDetailVO;
import com.aram.mayhem.dto.StrategyListVO;

import java.util.List;

/**
 * 攻略服务接口 —— 社区模块的"业务规则说明书"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个接口定义了"社区玩法模块"所有对外提供的业务功能。
 * 社区玩法模块允许用户发布、浏览、投票和删除游戏攻略。
 *
 * 打个比方：
 * - 攻略 = 玩家分享的游戏经验帖子（类似论坛帖子）
 * - 投票 = 帖子的"赞"和"踩"（帮助其他玩家判断攻略质量）
 * - 符文关联 = 攻略中推荐的强化符文
 * - 装备关联 = 攻略中推荐的装备
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、本接口提供的方法一览
 * ═══════════════════════════════════════════════════════════════════
 *
 * 方法名                  | 功能说明                      | 是否需要登录
 * ------------------------|------------------------------|------------
 * getStrategyList()       | 分页查询攻略列表              | 否
 * getStrategyDetail()     | 查看攻略详情                  | 否
 * createStrategy()        | 发布新攻略                    | 是
 * getUserStrategies()     | 查看某用户的攻略列表           | 否
 * upvoteStrategy()        | 点赞攻略                      | 是
 * downvoteStrategy()      | 踩攻略                        | 是
 * vote()                  | 投票（点赞或踩）              | 是
 * cancelVote()            | 取消投票                      | 是
 * deleteStrategy()        | 删除攻略（仅作者可删）         | 是
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、攻略数据结构
 * ═══════════════════════════════════════════════════════════════════
 *
 * 一篇攻略包含以下部分：
 *
 *   tb_strategy（攻略主体）
 *   ┌────┬────────┬────────┬──────────┬──────────┬──────────┐
 *   │ id │ userId │ heroId │  title   │description│ upvotes  │
 *   ├────┼────────┼────────┼──────────┼──────────┼──────────┤
 *   │  1 │   1    │   5    │盖伦出装  │ 10字以上  │   12     │
 *   └────┴────────┴────────┴──────────┴──────────┴──────────┘
 *         │          │
 *         │          └──────────► tb_hero（关联英雄）
 *         └─────────────────────► tb_user（发布用户）
 *
 *   tb_strategy_augment（攻略-符文关联）
 *   ┌────┬────────────┬────────────┐
 *   │ id │strategyId  │ augmentId  │
 *   ├────┼────────────┼────────────┤
 *   │  1 │    1       │    3       │  ← 攻略1推荐符文3
 *   │  2 │    1       │    7       │  ← 攻略1推荐符文7
 *   └────┴────────────┴────────────┘
 *
 *   tb_strategy_item（攻略-装备关联）
 *   ┌────┬────────────┬──────────┬────────────┐
 *   │ id │strategyId  │itemName  │ sortOrder  │
 *   ├────┼────────────┼──────────┼────────────┤
 *   │  1 │    1       │无尽之刃  │    0       │
 *   └────┴────────────┴──────────┴────────────┘
 *
 *   tb_vote（投票记录）
 *   ┌────┬────────────┬────────┬──────────┐
 *   │ id │strategyId  │ userId │ voteType │
 *   ├────┼────────────┼────────┼──────────┤
 *   │  1 │    1       │   2    │   UP     │  ← 用户2对攻略1点赞
 *   └────┴────────────┴────────┴──────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、实现类
 * ═══════════════════════════════════════════════════════════════════
 *
 * @see com.aram.mayhem.service.impl.StrategyServiceImpl 攻略服务实现类
 */
public interface StrategyService {

    /**
     * 获取攻略列表 —— 社区模块的核心查询方法
     *
     * ═══════════════════════════════════════════════════════════════
     * 功能说明
     * ═══════════════════════════════════════════════════════════════
     *
     * 分页查询攻略列表，支持按热度或时间排序。
     *
     * 【排序方式】
     * - "hot"    → 按点赞数从高到低排列（热门攻略优先）
     * - "latest" → 按发布时间从新到旧排列（最新攻略优先）
     * - 其他值   → 默认按发布时间排序
     *
     * ═══════════════════════════════════════════════════════════════
     * 参数说明
     * ═══════════════════════════════════════════════════════════════
     *
     * @param sort 排序方式（可选，传null默认按时间排序）
     *             可选值："hot"、"latest"
     * @param page 页码，从1开始（实现类会自动修正无效值）
     * @param size 每页数量（最大100，实现类会自动修正无效值）
     * @return List<StrategyListVO> 攻略列表，每个元素包含：
     *   - 攻略基本信息（id、标题、描述、点赞数、踩数、发布时间）
     *   - heroName → 关联英雄的中文名
     *   - heroIcon → 关联英雄的头像
     *   - authorNickname → 发布者昵称
     *   - authorAvatar → 发布者头像
     *   - score → 得分（点赞数 - 踩数）
     */
    List<StrategyListVO> getStrategyList(String sort, int page, int size);

    /**
     * 获取攻略详情 —— 查看单篇攻略的完整内容
     *
     * @param id 攻略ID
     * @return StrategyDetailVO 攻略详情，包含符文和装备关联信息；不存在返回null
     */
    StrategyDetailVO getStrategyDetail(Long id);

    /**
     * 创建攻略 —— 发布新的游戏攻略
     *
     * ═══════════════════════════════════════════════════════════════
     * 功能说明
     * ═══════════════════════════════════════════════════════════════
     *
     * 用户发布一篇新的游戏攻略。创建过程是事务性的（@Transactional），
     * 确保攻略主体、符文关联、装备关联同时写入成功，否则全部回滚。
     *
     * 【参数校验规则】
     * - 标题不能为空
     * - 描述至少10个字
     *
     * 【事务说明】
     * 为什么要用事务？因为创建攻略需要同时写入3张表：
     * 1. tb_strategy（攻略主体）
     * 2. tb_strategy_augment（符文关联）
     * 3. tb_strategy_item（装备关联）
     * 如果中间任何一步失败，前面已写入的数据也要撤销，保证数据一致性。
     *
     * @param userId      发布用户ID（从JWT Token中获取）
     * @param heroId      关联英雄ID（攻略针对哪个英雄）
     * @param title       攻略标题（不能为空）
     * @param description 攻略正文（至少10个字）
     * @param augmentIds  推荐符文ID列表（可选，传null或空列表表示不推荐符文）
     * @param itemIds     推荐装备ID列表（可选，传null或空列表表示不推荐装备）
     * @return StrategyDetailVO 创建后的攻略详情
     * @throws IllegalArgumentException 标题为空或描述不足10字时抛出
     */
    StrategyDetailVO createStrategy(Long userId, Long heroId, String title, String description,
                                     List<Long> augmentIds, List<Long> itemIds);

    /**
     * 获取指定用户的攻略列表 —— 查看某用户发布的所有攻略
     *
     * @param userId 用户ID
     * @return List<StrategyListVO> 该用户发布的攻略列表，按发布时间倒序
     */
    List<StrategyListVO> getUserStrategies(Long userId);

    /**
     * 点赞攻略 —— 增加攻略的点赞数
     *
     * @param strategyId 攻略ID
     */
    void upvoteStrategy(Long strategyId);

    /**
     * 踩攻略 —— 增加攻略的踩数
     *
     * @param strategyId 攻略ID
     */
    void downvoteStrategy(Long strategyId);

    /**
     * 投票 —— 用户对攻略进行点赞(UP)或点踩(DOWN)操作
     *
     * ═══════════════════════════════════════════════════════════════
     * 业务规则
     * ═══════════════════════════════════════════════════════════════
     *
     * 同一用户对同一攻略只能投一票：
     * - 如果还没投过 → 创建投票记录，更新攻略计数
     * - 如果已经投过 → 抛出 IllegalStateException，提示"已经投过票了"
     *
     * 如果想切换投票类型（从赞变踩或从踩变赞），需要先取消投票再重新投票。
     *
     * @param strategyId 攻略ID
     * @param userId     用户ID
     * @param voteType   投票类型："UP"（点赞）或 "DOWN"（踩）
     * @throws IllegalStateException 已投过票时抛出
     */
    void vote(Long strategyId, Long userId, String voteType);

    /**
     * 取消投票 —— 撤销之前对攻略的投票
     *
     * ═══════════════════════════════════════════════════════════════
     * 业务规则
     * ═══════════════════════════════════════════════════════════════
     *
     * - 如果该用户没有投过票 → 直接返回，不做任何操作
     * - 如果投过赞 → 删除投票记录，攻略 upvotes - 1（不低于0）
     * - 如果投过踩 → 删除投票记录，攻略 downvotes - 1（不低于0）
     *
     * @param strategyId 攻略ID
     * @param userId     用户ID
     */
    void cancelVote(Long strategyId, Long userId);

    /**
     * 删除攻略 —— 仅攻略作者可以删除自己的攻略
     *
     * ═══════════════════════════════════════════════════════════════
     * 业务规则
     * ═══════════════════════════════════════════════════════════════
     *
     * 删除攻略时，会同时删除所有关联数据：
     * 1. tb_strategy_augment 中该攻略的符文关联
     * 2. tb_strategy_item 中该攻略的装备关联
     * 3. tb_vote 中该攻略的所有投票记录
     * 4. tb_strategy 中攻略主体
     *
     * 【权限校验】
     * 只有攻略的作者（userId与攻略的userId一致）才能删除。
     * 非作者尝试删除会抛出 IllegalArgumentException。
     *
     * @param strategyId 攻略ID
     * @param userId     当前用户ID（用于权限校验）
     * @throws IllegalArgumentException 攻略不存在或无权删除时抛出
     */
    void deleteStrategy(Long strategyId, Long userId);
}
