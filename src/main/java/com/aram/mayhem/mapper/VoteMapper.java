package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.Vote;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 投票记录数据访问接口（Mapper） —— 投票数据的"数据库操作员"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个接口是"投票记录"数据和数据库之间的桥梁。
 * 它管理的是用户对攻略的投票（点赞或踩）记录。
 *
 * 投票系统的作用：
 * - 让用户表达对攻略的态度（认可或不认可）
 * - 通过点赞/踩数帮助其他用户筛选高质量攻略
 * - 防止低质量攻略排在前面
 *
 * 投票规则：
 * - 每个用户对每个攻略只能有一条投票记录（防重复投票）
 * - 用户可以切换投票（从赞切换为踩，或反过来）
 * - 投票时自动更新攻略的 upvotes/downvotes 计数
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、继承 BaseMapper<Vote> 后自动获得的方法
 * ═══════════════════════════════════════════════════════════════════
 *
 * 【查询方法 —— 最常用的操作】
 * - selectById(id)              → 根据 ID 查询单条投票记录
 * - selectOne(wrapper)          → 根据条件查询单条投票记录
 *   ★ 最常用：按 userId + strategyId 查询用户对某攻略的投票状态
 * - selectList(wrapper)         → 根据条件查询投票记录列表
 * - selectCount(wrapper)        → 统计符合条件的投票数量
 *
 * 【新增方法】
 * - insert(vote)                → 插入一条投票记录（首次投票）
 *
 * 【修改方法】
 * - updateById(vote)            → 根据 ID 更新投票记录（切换投票类型）
 *
 * 【删除方法】
 * - deleteById(id)              → 根据 ID 删除投票记录（取消投票）
 * - delete(wrapper)             → 根据条件删除
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、与其他 Mapper 的关系
 * ═══════════════════════════════════════════════════════════════════
 *
 * Vote 通过 strategyId 关联到 Strategy，通过 userId 关联到 User：
 *
 *   tb_user（用户表）         tb_vote（投票表）          tb_strategy（攻略表）
 *   ┌────┬──────┐            ┌────┬──────┬────────────┬──────┐   ┌────┬──────────┐
 *   │ id │name  │            │ id │userId│strategyId  │type  │   │ id │ title    │
 *   ├────┼──────┤            ├────┼──────┼────────────┼──────┤   ├────┼──────────┤
 *   │  1 │小明  │◄───────────│  1 │  1   │  5         │up    │──►│  5 │盖伦出装  │
 *   │  2 │小红  │◄───────────│  2 │  2   │  5         │down  │──►│    │          │
 *   └────┴──────┘            └────┴──────┴────────────┴──────┘   └────┴──────────┘
 *
 * 投票流程（在 StrategyService 中实现）：
 * 1. 用户点击"点赞"按钮
 * 2. 通过 VoteMapper 查询该用户是否已对该攻略投过票
 * 3. 如果没投过 → 插入新投票记录，攻略 upvotes + 1
 * 4. 如果已投过赞 → 取消投票，删除记录，攻略 upvotes - 1
 * 5. 如果已投过踩 → 切换为赞，更新 type，攻略 downvotes - 1, upvotes + 1
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、注解说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * @Mapper：告诉 MyBatis "这是一个 Mapper 接口，请为它创建实现类"。
 *          Spring Boot 启动时会扫描所有 @Mapper 注解的接口，
 *          自动为每个接口生成一个代理类，代理类包含了所有 SQL 操作。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * 数据库 tb_vote 表
 *     ↕（MyBatis-Plus 自动映射）
 * VoteMapper（本接口）
 *     ↕（StrategyService 调用 Mapper 方法）
 * StrategyService / StrategyServiceImpl
 *     ↕（投票逻辑：查重、插入/更新/删除投票记录，更新攻略计数）
 * VoteController / StrategyController
 *     ↕（HTTP API 返回 JSON）
 * Android 前端（攻略详情页的点赞/踩按钮）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、关联类
 * ═══════════════════════════════════════════════════════════════════
 *
 * - Vote：投票记录实体类，对应 tb_vote 表
 * - Strategy：攻略实体类，投票通过 strategyId 关联到攻略
 * - User：用户实体类，投票通过 userId 关联到用户
 * - StrategyService / StrategyServiceImpl：投票的业务逻辑层
 * - VoteController：投票的 API 接口层
 * - BaseMapper<Vote>：MyBatis-Plus 提供的基础 CRUD 接口
 *
 * ═══════════════════════════════════════════════════════════════════
 * 七、使用示例
 * ═══════════════════════════════════════════════════════════════════
 *
 * 在 Service 层中的典型用法：
 *
 *   // 1. 查询用户对某攻略的投票状态（判断按钮是"已赞"还是"未赞"）
 *   LambdaQueryWrapper<Vote> wrapper = new LambdaQueryWrapper<>();
 *   wrapper.eq(Vote::getUserId, userId)
 *          .eq(Vote::getStrategyId, strategyId);
 *   Vote existingVote = voteMapper.selectOne(wrapper);
 *
 *   // 2. 首次投票 —— 插入新记录
 *   Vote vote = new Vote();
 *   vote.setUserId(userId);
 *   vote.setStrategyId(strategyId);
 *   vote.setType("up");  // 或 "down"
 *   voteMapper.insert(vote);
 *
 *   // 3. 切换投票 —— 更新 type 字段
 *   existingVote.setType("down");  // 从赞切换为踩
 *   voteMapper.updateById(existingVote);
 *
 *   // 4. 取消投票 —— 删除记录
 *   voteMapper.deleteById(existingVote.getId());
 */
@Mapper // 告诉 MyBatis 这是一个 Mapper 接口，启动时自动生成实现类
public interface VoteMapper extends BaseMapper<Vote> {
    // 当前不需要自定义方法，BaseMapper<Vote> 已提供所有基础 CRUD 操作
    // 最常用的操作是按 userId + strategyId 查询投票状态，通过 LambdaQueryWrapper 即可实现
}
