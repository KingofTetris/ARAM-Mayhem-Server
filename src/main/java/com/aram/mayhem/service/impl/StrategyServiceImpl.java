package com.aram.mayhem.service.impl;

import com.aram.mayhem.dto.*;
import com.aram.mayhem.entity.*;
import com.aram.mayhem.mapper.*;
import com.aram.mayhem.service.StrategyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 攻略服务实现类 —— 社区模块的"后厨"，真正做菜的地方
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类实现了 StrategyService 接口，是社区模块所有业务逻辑的"真正执行者"。
 * StrategyService 接口只定义了"有哪些方法"（菜单），这个类负责"具体怎么做"（做菜）。
 *
 * 社区模块是 ARAM Mayhem Assistant 的核心社交功能之一，允许用户：
 * - 发布自己的游戏攻略（比如"亚索必选符文搭配"）
 * - 浏览其他用户的攻略列表
 * - 对攻略进行投票（点赞/踩）
 * - 删除自己发布的攻略
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、这个类依赖了哪些"帮手"？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 依赖对象                  | 作用                                | 打个比方
 * --------------------------|-------------------------------------|------------------
 * StrategyMapper            | 操作 tb_strategy 数据库表            | 仓库管理员（取攻略数据）
 * StrategyAugmentMapper     | 操作 tb_strategy_augment 关联表      | 仓库管理员（取攻略-符文关联）
 * StrategyItemMapper        | 操作 tb_strategy_item 关联表         | 仓库管理员（取攻略-装备关联）
 * HeroMapper                | 操作 tb_hero 数据库表                | 仓库管理员（取英雄数据）
 * UserMapper                | 操作 tb_user 数据库表                | 仓库管理员（取用户数据）
 * VoteMapper                | 操作 tb_vote 数据库表                | 仓库管理员（取投票记录）
 * AugmentMapper             | 操作 tb_augment 数据库表             | 仓库管理员（取符文数据）
 * StringRedisTemplate       | 操作 Redis 缓存                      | 快速缓存读写
 * Logger (log)              | 记录运行日志                         | 工作记录本
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据库表关系说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 攻略模块涉及多张数据库表，它们之间的关系如下：
 *
 * tb_strategy（攻略主表）
 *   ├── tb_strategy_augment（攻略-符文关联表，一对多）
 *   │     └── 一个攻略可以推荐多个符文
 *   ├── tb_strategy_item（攻略-装备关联表，一对多）
 *   │     └── 一个攻略可以推荐多个装备
 *   └── tb_vote（投票记录表，一对多）
 *         └── 一个攻略可以被多个用户投票
 *
 * tb_strategy 通过 hero_id 关联 tb_hero（攻略属于哪个英雄）
 * tb_strategy 通过 user_id 关联 tb_user（攻略是谁发布的）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、事务管理说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本类中以下方法使用了 @Transactional 注解，确保数据库操作的原子性：
 *
 * 方法                    | 事务原因
 * ------------------------|--------------------------------------------------
 * createStrategy()        | 需同时写入攻略主体 + 符文关联 + 装备关联
 * vote()                  | 需同时写入投票记录 + 更新攻略计数
 * cancelVote()            | 需同时删除投票记录 + 恢复攻略计数
 * deleteStrategy()        | 需同时删除攻略主体 + 关联数据 + 投票记录
 * upvoteStrategy()        | 需确保计数一致性
 * downvoteStrategy()      | 需确保计数一致性
 *
 * 如果事务中任何一步失败，所有已执行的数据库操作都会自动回滚，
 * 保证数据不会出现"攻略创建了但符文没关联上"这种不一致的状态。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、投票业务规则说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 投票系统遵循以下规则：
 * 1. 每个用户对同一篇攻略只能投一票（UP 或 DOWN）
 * 2. 重复投票会抛出 IllegalStateException
 * 3. 用户可以取消自己的投票，取消后计数恢复
 * 4. 计数不会低于0（使用 Math.max(0, ...) 保护）
 *
 * 投票流程图：
 * ┌──────────┐     检查是否已投票      ┌──────────┐
 * │ 用户点击  │ ──────────────────────→ │ 已投票？  │
 * │ 投票按钮  │                         └────┬─────┘
 * └──────────┘                               │
 *                                   ┌────────┴────────┐
 *                                   │                 │
 *                                  是                否
 *                                   │                 │
 *                                   ▼                 ▼
 *                           ┌──────────┐      ┌──────────┐
 *                           │ 抛出异常  │      │ 创建投票  │
 *                           │ "已投过票"│      │ 更新计数  │
 *                           └──────────┘      └──────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、排序策略说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 攻略列表支持两种排序方式：
 * - hot（热度排序）：按点赞数（upvotes）从高到低排列
 *   → 适合发现最受欢迎的攻略
 * - latest（最新排序，默认）：按发布时间（createdAt）从新到旧排列
 *   → 适合查看最新发布的攻略
 *
 * ═══════════════════════════════════════════════════════════════════
 * 七、方法总览
 * ═══════════════════════════════════════════════════════════════════
 *
 * 方法名                  | 功能                    | 是否事务
 * ------------------------|-------------------------|----------
 * getStrategyList()       | 分页查询攻略列表         | 否
 * getStrategyDetail()     | 查询攻略详情             | 否
 * createStrategy()        | 创建攻略（含关联数据）   | 是
 * getUserStrategies()     | 查询用户的攻略列表       | 否
 * upvoteStrategy()        | 点赞攻略                 | 是
 * downvoteStrategy()      | 踩攻略                   | 是
 * vote()                  | 投票（含重复检查）       | 是
 * cancelVote()            | 取消投票                 | 是
 * deleteStrategy()        | 删除攻略（含关联数据）   | 是
 * convertToListVO()       | 实体→列表VO转换（私有）  | 否
 * convertToDetailVO()     | 实体→详情VO转换（私有）  | 否
 */
@Service
public class StrategyServiceImpl implements StrategyService {

    /**
     * 日志记录器 —— 用来记录程序运行过程中的关键信息
     *
     * 为什么需要日志？
     * - 在开发阶段：帮助调试，看程序执行到了哪一步
     * - 在生产环境：当用户反馈问题时，可以通过日志定位原因
     *
     * 日志级别从低到高：TRACE → DEBUG → INFO → WARN → ERROR
     * 本类主要使用 INFO 级别记录关键操作（创建、删除等）
     */
    private static final Logger log = LoggerFactory.getLogger(StrategyServiceImpl.class);

    /**
     * 攻略数据访问对象 —— 负责与 tb_strategy 表交互
     *
     * 主要操作：
     * - selectPage()     → 分页查询攻略列表
     * - selectById()     → 根据ID查询单条攻略
     * - insert()         → 插入新攻略
     * - updateById()     → 根据ID更新攻略（用于更新投票计数）
     * - deleteById()     → 根据ID删除攻略
     *
     * 对应数据库表：tb_strategy
     * 主要字段：id, user_id, hero_id, title, description, upvotes, downvotes, created_at
     */
    private final StrategyMapper strategyMapper;

    /**
     * 攻略-符文关联数据访问对象 —— 负责与 tb_strategy_augment 表交互
     *
     * 主要操作：
     * - insert()         → 为攻略添加符文关联
     * - delete()         → 删除攻略的所有符文关联
     *
     * 对应数据库表：tb_strategy_augment
     * 主要字段：id, strategy_id, augment_id
     *
     * 为什么需要关联表？
     * 因为一个攻略可以推荐多个符文，这是"一对多"关系。
     * 不能把符文ID直接存在攻略表里（那样只能存一个），所以用独立的关联表。
     */
    private final StrategyAugmentMapper strategyAugmentMapper;

    /**
     * 攻略-装备关联数据访问对象 —— 负责与 tb_strategy_item 表交互
     *
     * 主要操作：
     * - insert()         → 为攻略添加装备关联
     * - delete()         → 删除攻略的所有装备关联
     *
     * 对应数据库表：tb_strategy_item
     * 主要字段：id, strategy_id, item_name, item_category, sort_order
     *
     * 与符文关联表类似，一个攻略可以推荐多个装备，使用独立关联表存储。
     */
    private final StrategyItemMapper strategyItemMapper;

    /**
     * 英雄数据访问对象 —— 用于查询攻略关联的英雄信息
     *
     * 在本类中的用途：
     * - 查询攻略所属英雄的中文名称和头像URL
     * - 这些信息会填充到 StrategyListVO 和 StrategyDetailVO 中
     *
     * 对应数据库表：tb_hero
     */
    private final HeroMapper heroMapper;

    /**
     * 用户数据访问对象 —— 用于查询攻略作者的用户信息
     *
     * 在本类中的用途：
     * - 查询攻略发布者的昵称和头像URL
     * - 这些信息会填充到 StrategyListVO 和 StrategyDetailVO 中
     *
     * 对应数据库表：tb_user
     */
    private final UserMapper userMapper;

    /**
     * 投票记录数据访问对象 —— 负责与 tb_vote 表交互
     *
     * 主要操作：
     * - selectOne()      → 查询用户对某攻略的投票记录（用于重复投票检查）
     * - insert()         → 插入新的投票记录
     * - delete()         → 删除投票记录（取消投票时）
     *
     * 对应数据库表：tb_vote
     * 主要字段：id, strategy_id, user_id, vote_type
     *
     * vote_type 取值：
     * - "UP"   → 点赞
     * - "DOWN" → 踩
     */
    private final VoteMapper voteMapper;

    /**
     * 符文数据访问对象 —— 用于查询符文详细信息
     *
     * 在本类中的用途：
     * - 查询攻略关联的符文名称和描述
     * - 这些信息会填充到 StrategyDetailVO 中
     *
     * 对应数据库表：tb_augment
     */
    private final AugmentMapper augmentMapper;

    /**
     * Redis 操作模板 —— 用于缓存操作
     *
     * 在本类中的用途：
     * - 预留的缓存能力，未来可用于缓存热门攻略列表
     * - 目前主要作为依赖注入预留，尚未深度使用
     *
     * StringRedisTemplate vs RedisTemplate 的区别：
     * - StringRedisTemplate 专门处理字符串类型的键值对
     * - RedisTemplate 可以处理任意 Java 对象（需要序列化配置）
     */
    private final StringRedisTemplate redisTemplate;

    /**
     * 构造函数 —— Spring 自动注入所有依赖
     *
     * ══════════════════════════════════════════════════════════════
     * 什么是依赖注入？
     * ══════════════════════════════════════════════════════════════
     *
     * 传统方式：我们需要自己创建对象，比如 new StrategyMapper()
     * Spring方式：我们在构造函数中声明需要什么，Spring 会自动把对象传进来
     *
     * 好处：
     * 1. 不需要关心对象怎么创建的
     * 2. 方便替换实现（比如测试时用 Mock 替代真实数据库操作）
     * 3. 对象的生命周期由 Spring 统一管理
     *
     * @param strategyMapper        攻略数据访问对象
     * @param strategyAugmentMapper 攻略-符文关联数据访问对象
     * @param strategyItemMapper    攻略-装备关联数据访问对象
     * @param heroMapper            英雄数据访问对象
     * @param userMapper            用户数据访问对象
     * @param voteMapper            投票记录数据访问对象
     * @param augmentMapper         符文数据访问对象
     * @param redisTemplate         Redis操作模板
     */
    @Autowired
    public StrategyServiceImpl(StrategyMapper strategyMapper,
                                StrategyAugmentMapper strategyAugmentMapper,
                                StrategyItemMapper strategyItemMapper,
                                HeroMapper heroMapper,
                                UserMapper userMapper,
                                VoteMapper voteMapper,
                                AugmentMapper augmentMapper,
                                StringRedisTemplate redisTemplate) {
        this.strategyMapper = strategyMapper;
        this.strategyAugmentMapper = strategyAugmentMapper;
        this.strategyItemMapper = strategyItemMapper;
        this.heroMapper = heroMapper;
        this.userMapper = userMapper;
        this.voteMapper = voteMapper;
        this.augmentMapper = augmentMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取攻略列表 —— 社区模块的核心浏览功能
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 当用户打开社区页面时，需要展示一个攻略列表。
     * 这个方法就是负责从数据库中查询攻略数据，并按照用户指定的排序方式返回。
     *
     * 就像逛论坛一样：
     * - 你可以按"最热"排序（看大家都在讨论什么）
     * - 也可以按"最新"排序（看有没有新帖子）
     *
     * ══════════════════════════════════════════════════════════════
     * 参数说明
     * ══════════════════════════════════════════════════════════════
     *
     * @param sort 排序方式，支持两种值：
     *             - "hot"    → 按点赞数从高到低排列（热门优先）
     *             - 其他值    → 按发布时间从新到旧排列（默认，最新优先）
     *
     * @param page 页码，从1开始（第1页、第2页...）
     *             - 如果传入0或负数，自动修正为1
     *
     * @param size 每页显示的攻略数量
     *             - 如果传入0或负数，自动修正为10
     *             - 最大不超过100（防止一次查询太多数据导致性能问题）
     *
     * @return List<StrategyListVO> 攻略列表，每个元素包含：
     *         - 攻略ID、标题、描述
     *         - 点赞数、踩数、得分（点赞-踩）
     *         - 关联英雄的名称和头像
     *         - 作者的昵称和头像
     *         - 发布时间
     *
     * ══════════════════════════════════════════════════════════════
     * 执行流程
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 参数边界校验 → 防止非法参数导致数据库查询异常
     * 2. 创建分页参数 → 告诉 MyBatis-Plus 查第几页、每页几条
     * 3. 构建查询条件 → 决定按什么字段排序
     * 4. 执行分页查询 → 从数据库获取数据
     * 5. 转换为VO对象 → 填充英雄名称、作者昵称等关联信息
     *
     * ══════════════════════════════════════════════════════════════
     * 为什么返回 List 而不是 Page 对象？
     * ══════════════════════════════════════════════════════════════
     *
     * MyBatis-Plus 的 selectPage() 返回的是 Page<Strategy> 对象，
     * 但我们只取其中的记录列表（getRecords()），然后转换为 VO 列表返回。
     * 这样做的好处是：
     * - Controller 层不需要关心分页的底层实现
     * - 返回的数据格式更简洁
     * - 前端只需要关心攻略列表本身
     */
    @Override
    public List<StrategyListVO> getStrategyList(String sort, int page, int size) {
        // ─── 参数边界校验 ───
        // 为什么需要校验？
        // 如果 page=0 或 page=-1 传给 MyBatis-Plus，会导致 SQL 语法错误
        // 如果 size=1000，一次查询1000条数据会严重影响性能
        if (page <= 0) page = 1;    // 页码最小为1
        if (size <= 0) size = 10;   // 每页数量最小为10
        if (size > 100) size = 100; // 每页数量最大为100

        // ─── 创建分页参数 ───
        // Page<Strategy> 是 MyBatis-Plus 提供的分页对象
        // 构造函数参数：page=当前页码，size=每页条数
        // 它最终会被转换为 SQL 的 LIMIT 语句
        // 例如 page=2, size=10 → LIMIT 10, 10（跳过前10条，取接下来的10条）
        Page<Strategy> pageParam = new Page<>(page, size);

        // ─── 构建查询条件 ───
        // LambdaQueryWrapper 是 MyBatis-Plus 的条件构造器
        // 使用 Lambda 表达式（如 Strategy::getUpvotes）而不是字符串（如 "upvotes"）
        // 好处：编译期检查字段名，如果字段名拼错会直接报错而不是运行时才发现
        LambdaQueryWrapper<Strategy> wrapper = new LambdaQueryWrapper<>();

        // ─── 排序逻辑 ───
        // "hot" → 按点赞数降序排列，点赞最多的排在最前面
        // 其他   → 按发布时间降序排列，最新发布的排在最前面
        // orderByDesc() 生成 SQL 的 ORDER BY xxx DESC
        if ("hot".equalsIgnoreCase(sort)) {
            wrapper.orderByDesc(Strategy::getUpvotes);
        } else {
            wrapper.orderByDesc(Strategy::getCreatedAt);
        }

        // ─── 执行分页查询 ───
        // selectPage() 会执行两条 SQL：
        // 1. SELECT COUNT(*) FROM tb_strategy（查询总数，用于计算总页数）
        // 2. SELECT * FROM tb_strategy ORDER BY xxx DESC LIMIT offset, size（查询当前页数据）
        Page<Strategy> result = strategyMapper.selectPage(pageParam, wrapper);

        // ─── 转换为VO并返回 ───
        // result.getRecords() 获取当前页的数据列表
        // stream().map() 对每条记录执行 convertToListVO() 转换
        // collect(Collectors.toList()) 将 Stream 收集为 List
        return result.getRecords().stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取攻略详情 —— 查看一篇攻略的完整内容
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 当用户在攻略列表中点击某篇攻略时，需要展示该攻略的详细信息。
     * 这个方法根据攻略ID查询完整的攻略数据，包括关联的英雄和作者信息。
     *
     * 与 getStrategyList() 的区别：
     * - getStrategyList() 返回的是列表摘要（标题、点赞数等）
     * - getStrategyDetail() 返回的是完整详情（包含描述全文等）
     *
     * @param id 攻略ID，对应数据库中的主键
     * @return StrategyDetailVO 攻略详情对象，包含：
     *         - 攻略ID、标题、描述全文
     *         - 点赞数、踩数
     *         - 关联英雄的名称和头像
     *         - 作者的昵称和头像
     *         - 发布时间
     *         如果攻略不存在，返回 null
     *
     * ══════════════════════════════════════════════════════════════
     * 为什么返回 null 而不是抛异常？
     * ══════════════════════════════════════════════════════════════
     *
     * 这是一种"温和"的处理方式：
     * - Controller 层可以根据返回值是否为 null 来决定返回 404 还是 200
     * - 如果直接抛异常，Controller 层就必须 try-catch，代码更复杂
     * - 但也有缺点：调用者可能忘记检查 null，导致 NullPointerException
     */
    @Override
    public StrategyDetailVO getStrategyDetail(Long id) {
        // ─── 根据ID查询攻略 ───
        // selectById() 生成 SQL：SELECT * FROM tb_strategy WHERE id = ?
        Strategy strategy = strategyMapper.selectById(id);

        // ─── 攻略不存在则返回null ───
        // 如果用户传入了一个不存在的ID，数据库查不到数据，返回null
        if (strategy == null) {
            return null;
        }

        // ─── 转换为详情VO ───
        // convertToDetailVO() 会填充英雄名称、作者昵称等关联信息
        return convertToDetailVO(strategy);
    }

    /**
     * 创建攻略 —— 用户发布新攻略的核心方法
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 当用户在社区页面点击"发布攻略"按钮时，前端会调用这个方法。
     * 它负责创建一篇新攻略，包括：
     * 1. 攻略主体（标题、描述、关联英雄等）
     * 2. 推荐符文关联（一个攻略可以推荐多个符文）
     * 3. 推荐装备关联（一个攻略可以推荐多个装备）
     *
     * ══════════════════════════════════════════════════════════════
     * 为什么需要 @Transactional？
     * ══════════════════════════════════════════════════════════════
     *
     * 创建攻略涉及多张表的操作：
     * - 第1步：插入 tb_strategy（攻略主体）
     * - 第2步：插入 tb_strategy_augment（符文关联，可能多条）
     * - 第3步：插入 tb_strategy_item（装备关联，可能多条）
     *
     * 如果第2步或第3步失败了，但第1步已经成功，就会出现"有攻略但没有符文"的不一致数据。
     * @Transactional 保证：要么全部成功，要么全部回滚，不会出现中间状态。
     *
     * ══════════════════════════════════════════════════════════════
     * 参数说明
     * ══════════════════════════════════════════════════════════════
     *
     * @param userId      发布用户的ID（从JWT Token中获取，不是前端传的）
     * @param heroId      关联英雄的ID（这篇攻略是关于哪个英雄的）
     * @param title       攻略标题（不能为空）
     * @param description 攻略正文（至少10个字，防止灌水）
     * @param augmentIds  推荐符文ID列表（可以为空，表示不推荐符文）
     * @param itemIds     推荐装备ID列表（可以为空，表示不推荐装备）
     *
     * @return StrategyDetailVO 创建成功后的攻略详情
     *
     * @throws IllegalArgumentException 当参数校验失败时抛出：
     *         - 标题为空 → "标题不能为空"
     *         - 描述不足10字 → "描述至少需要10个字"
     *
     * ══════════════════════════════════════════════════════════════
     * 执行流程图
     * ══════════════════════════════════════════════════════════════
     *
     * ┌─────────────┐
     * │ 参数校验     │ ──→ 标题为空？描述太短？──→ 抛出异常
     * └──────┬──────┘
     *        │ 通过
     *        ▼
     * ┌─────────────┐
     * │ 创建攻略主体 │ ──→ INSERT INTO tb_strategy
     * └──────┬──────┘
     *        │ 成功（获得自增ID）
     *        ▼
     * ┌─────────────┐
     * │ 关联符文     │ ──→ INSERT INTO tb_strategy_augment（循环）
     * └──────┬──────┘
     *        │
     *        ▼
     * ┌─────────────┐
     * │ 关联装备     │ ──→ INSERT INTO tb_strategy_item（循环）
     * └──────┬──────┘
     *        │
     *        ▼
     * ┌─────────────┐
     * │ 返回详情VO   │
     * └─────────────┘
     */
    @Override
    @Transactional
    public StrategyDetailVO createStrategy(Long userId, Long heroId, String title, String description,
                                           List<Long> augmentIds, List<Long> itemIds) {
        // ─── 记录操作日志 ───
        // 在创建攻略时记录关键参数，方便排查问题
        // 例如：如果用户反馈"我发布的攻略不见了"，可以通过日志查看是否创建成功
        log.info("Creating strategy: userId={}, heroId={}, title={}", userId, heroId, title);

        // ─── 参数校验 ───
        // 标题不能为空或纯空格
        // title.trim() 会去掉首尾空格，如果去掉后为空说明用户只输入了空格
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
        }

        // 描述至少需要10个字
        // 这是为了防止用户发布只有一两个字的"灌水"攻略
        // description.trim().length() 计算去掉首尾空格后的字符数
        if (description == null || description.trim().length() < 10) {
            throw new IllegalArgumentException("描述至少需要10个字");
        }

        // ─── 创建攻略主体对象 ───
        // 这里只是创建 Java 对象，还没有写入数据库
        // 类比：你填好了一张表单，但还没提交
        Strategy strategy = new Strategy();
        strategy.setUserId(userId);           // 设置发布者ID
        strategy.setHeroId(heroId);           // 设置关联英雄ID
        strategy.setTitle(title.trim());      // 设置标题（去掉首尾空格）
        strategy.setDescription(description.trim()); // 设置描述（去掉首尾空格）
        strategy.setUpvotes(0);               // 初始点赞数为0
        strategy.setDownvotes(0);             // 初始踩数为0

        // ─── 插入攻略主体到数据库 ───
        // insert() 生成 SQL：INSERT INTO tb_strategy (user_id, hero_id, title, description, upvotes, downvotes) VALUES (?, ?, ?, ?, 0, 0)
        // 插入成功后，strategy.getId() 会自动获取数据库生成的自增ID
        // 这个ID后面关联符文和装备时需要用到
        strategyMapper.insert(strategy);
        log.info("Strategy created: strategyId={}, userId={}", strategy.getId(), userId);

        // ─── 关联符文 ───
        // 如果用户选择了推荐符文，就逐个插入关联记录
        // augmentIds 可能为 null（用户没选符文）或空列表（用户取消了所有选择）
        if (augmentIds != null && !augmentIds.isEmpty()) {
            for (Long augmentId : augmentIds) {
                // 创建攻略-符文关联对象
                StrategyAugment sa = new StrategyAugment();
                sa.setStrategyId(strategy.getId()); // 关联到刚创建的攻略
                sa.setAugmentId(augmentId);          // 关联到指定的符文

                // 插入关联记录
                // 生成 SQL：INSERT INTO tb_strategy_augment (strategy_id, augment_id) VALUES (?, ?)
                strategyAugmentMapper.insert(sa);
            }
        }

        // ─── 关联装备 ───
        // 如果用户选择了推荐装备，就逐个插入关联记录
        // 逻辑与关联符文完全相同
        if (itemIds != null && !itemIds.isEmpty()) {
            for (Long itemId : itemIds) {
                // 创建攻略-装备关联对象
                StrategyItem si = new StrategyItem();
                si.setStrategyId(strategy.getId()); // 关联到刚创建的攻略
                si.setItemName("Item-" + itemId);    // 装备名称（目前使用ID拼接，后续可优化）
                si.setItemCategory("build");          // 装备分类（固定为"build"表示出装）
                si.setSortOrder(0);                   // 排序序号（目前固定为0）

                // 插入关联记录
                // 生成 SQL：INSERT INTO tb_strategy_item (strategy_id, item_name, item_category, sort_order) VALUES (?, ?, ?, ?)
                strategyItemMapper.insert(si);
            }
        }

        // ─── 返回创建后的攻略详情 ───
        // convertToDetailVO() 会查询关联的英雄和作者信息，填充到VO中
        return convertToDetailVO(strategy);
    }

    /**
     * 获取用户发布的攻略列表 —— 个人中心"我的攻略"功能
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 当用户进入个人中心页面，想查看自己发布过的所有攻略时，
     * 前端会调用这个方法，传入用户ID，获取该用户的所有攻略列表。
     *
     * 与 getStrategyList() 的区别：
     * - getStrategyList() 查询所有用户的攻略（社区首页）
     * - getUserStrategies() 只查询指定用户的攻略（个人中心）
     *
     * @param userId 用户ID
     * @return List<StrategyListVO> 该用户发布的攻略列表，按发布时间倒序排列
     *
     * ══════════════════════════════════════════════════════════════
     * 为什么没有分页？
     * ══════════════════════════════════════════════════════════════
     *
     * 目前假设普通用户发布的攻略数量不会太多（通常几十条），
     * 所以直接查询全部返回。如果未来数据量增大，可以改为分页查询。
     */
    @Override
    public List<StrategyListVO> getUserStrategies(Long userId) {
        // ─── 构建查询条件 ───
        // eq(Strategy::getUserId, userId) 生成 SQL：WHERE user_id = ?
        // orderByDesc(Strategy::getCreatedAt) 生成 SQL：ORDER BY created_at DESC
        LambdaQueryWrapper<Strategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Strategy::getUserId, userId)
                .orderByDesc(Strategy::getCreatedAt);

        // ─── 查询并转换为VO ───
        // selectList() 查询所有符合条件的记录（不分页）
        return strategyMapper.selectList(wrapper).stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());
    }

    /**
     * 点赞攻略 —— 简单版点赞（无重复检查）
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 增加攻略的点赞计数（upvotes + 1）。
     * 这是简化版的点赞方法，不检查用户是否已经点过赞。
     *
     * ══════════════════════════════════════════════════════════════
     * ⚠️ 注意：此方法不检查重复点赞！
     * ══════════════════════════════════════════════════════════════
     *
     * 如果需要防止重复点赞，请使用 vote() 方法代替。
     * vote() 方法会检查用户是否已投票，防止重复操作。
     *
     * @param strategyId 攻略ID
     */
    @Override
    @Transactional
    public void upvoteStrategy(Long strategyId) {
        // ─── 查询攻略 ───
        Strategy strategy = strategyMapper.selectById(strategyId);

        // ─── 攻略存在则更新计数 ───
        if (strategy != null) {
            strategy.setUpvotes(strategy.getUpvotes() + 1); // 点赞数+1
            strategyMapper.updateById(strategy);              // 更新到数据库
        }
    }

    /**
     * 踩攻略 —— 简单版踩（无重复检查）
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 增加攻略的踩计数（downvotes + 1）。
     * 这是简化版的踩方法，不检查用户是否已经踩过。
     *
     * ══════════════════════════════════════════════════════════════
     * ⚠️ 注意：此方法不检查重复踩！
     * ══════════════════════════════════════════════════════════════
     *
     * 如果需要防止重复踩，请使用 vote() 方法代替。
     *
     * @param strategyId 攻略ID
     */
    @Override
    @Transactional
    public void downvoteStrategy(Long strategyId) {
        // ─── 查询攻略 ───
        Strategy strategy = strategyMapper.selectById(strategyId);

        // ─── 攻略存在则更新计数 ───
        if (strategy != null) {
            strategy.setDownvotes(strategy.getDownvotes() + 1); // 踩数+1
            strategyMapper.updateById(strategy);                  // 更新到数据库
        }
    }

    /**
     * 投票 —— 完整版投票（含重复检查）
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 用户对攻略进行投票（点赞或踩），同时记录投票记录到 tb_vote 表。
     * 与 upvoteStrategy()/downvoteStrategy() 不同，这个方法会：
     * 1. 检查用户是否已经投过票（防止重复投票）
     * 2. 在 tb_vote 表中创建投票记录
     * 3. 更新攻略的点赞/踩计数
     *
     * ══════════════════════════════════════════════════════════════
     * 为什么需要 @Transactional？
     * ══════════════════════════════════════════════════════════════
     *
     * 投票涉及两个数据库操作：
     * 1. INSERT INTO tb_vote（创建投票记录）
     * 2. UPDATE tb_strategy（更新点赞/踩计数）
     *
     * 如果第2步失败了，但第1步已经成功，就会出现"有投票记录但计数没更新"的不一致。
     * @Transactional 保证两步要么都成功，要么都回滚。
     *
     * @param strategyId 攻略ID
     * @param userId     用户ID
     * @param voteType   投票类型："UP"（点赞）或 "DOWN"（踩）
     *
     * @throws IllegalStateException 当用户已经对这篇攻略投过票时抛出
     *
     * ══════════════════════════════════════════════════════════════
     * 执行流程
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 检查是否已投票 → 已投票则抛异常
     * 2. 查询攻略 → 不存在则直接返回
     * 3. 创建投票记录 → INSERT INTO tb_vote
     * 4. 更新攻略计数 → UPDATE tb_strategy SET upvotes/downvotes = ?
     *
     * ══════════════════════════════════════════════════════════════
     * 并发安全问题
     * ══════════════════════════════════════════════════════════════
     *
     * ⚠️ 当前实现存在并发问题：
     * 如果两个请求同时到达，都检查到"未投票"，然后都插入投票记录，
     * 就会出现同一用户对同一攻略投了两票的情况。
     *
     * 解决方案（未来优化）：
     * 1. 在 tb_vote 表上添加 UNIQUE 约束 (strategy_id, user_id)
     * 2. 使用 Redis 分布式锁
     * 3. 使用数据库乐观锁
     */
    @Override
    @Transactional
    public void vote(Long strategyId, Long userId, String voteType) {
        // ─── 记录操作日志 ───
        log.info("Vote: strategyId={}, userId={}, voteType={}", strategyId, userId, voteType);

        // ─── 检查是否已投票 ───
        // 构建查询条件：WHERE strategy_id = ? AND user_id = ?
        // 如果查到记录，说明该用户已经对这篇攻略投过票了
        LambdaQueryWrapper<Vote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Vote::getStrategyId, strategyId)
                .eq(Vote::getUserId, userId);

        Vote existingVote = voteMapper.selectOne(wrapper);

        // ─── 已投票则抛出异常 ───
        // 这是一条业务规则：每个用户对每篇攻略只能投一票
        if (existingVote != null) {
            throw new IllegalStateException("已经投过票了");
        }

        // ─── 查询攻略 ───
        // 需要确认攻略存在，以及获取当前的投票计数
        Strategy strategy = strategyMapper.selectById(strategyId);
        if (strategy == null) {
            // 攻略不存在，直接返回（不抛异常，因为这不是用户的错）
            return;
        }

        // ─── 创建投票记录 ───
        // 在 tb_vote 表中插入一条新记录
        Vote vote = new Vote();
        vote.setStrategyId(strategyId);  // 关联到哪篇攻略
        vote.setUserId(userId);           // 谁投的票
        vote.setVoteType(voteType);       // 投的什么票（UP/DOWN）
        voteMapper.insert(vote);

        // ─── 更新攻略计数 ───
        // 根据投票类型，增加对应的计数
        if ("UP".equalsIgnoreCase(voteType)) {
            strategy.setUpvotes(strategy.getUpvotes() + 1);   // 点赞数+1
        } else if ("DOWN".equalsIgnoreCase(voteType)) {
            strategy.setDownvotes(strategy.getDownvotes() + 1); // 踩数+1
        }
        strategyMapper.updateById(strategy);
    }

    /**
     * 取消投票 —— 撤回之前的投票
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 当用户想撤回之前对某篇攻略的投票时，调用这个方法。
     * 它会：
     * 1. 删除 tb_vote 中的投票记录
     * 2. 恢复攻略的点赞/踩计数（减1）
     *
     * ══════════════════════════════════════════════════════════════
     * 为什么需要 @Transactional？
     * ══════════════════════════════════════════════════════════════
     *
     * 取消投票涉及两个数据库操作：
     * 1. DELETE FROM tb_vote（删除投票记录）
     * 2. UPDATE tb_strategy（恢复计数）
     *
     * 必须保证两步同时成功或同时失败，否则会出现数据不一致。
     *
     * @param strategyId 攻略ID
     * @param userId     用户ID
     *
     * ══════════════════════════════════════════════════════════════
     * 计数保护说明
     * ══════════════════════════════════════════════════════════════
     *
     * 使用 Math.max(0, count - 1) 确保计数不会变成负数。
     * 为什么可能出现负数？
     * - 如果数据库中的计数已经被手动修改过
     * - 如果存在并发问题导致计数被多次减少
     * - 防御性编程：即使出了问题，也不要让数据看起来更奇怪
     */
    @Override
    @Transactional
    public void cancelVote(Long strategyId, Long userId) {
        // ─── 查询投票记录 ───
        // WHERE strategy_id = ? AND user_id = ?
        LambdaQueryWrapper<Vote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Vote::getStrategyId, strategyId)
                .eq(Vote::getUserId, userId);

        Vote vote = voteMapper.selectOne(wrapper);

        // ─── 没有投票记录则直接返回 ───
        // 用户没有投过票，不需要取消，静默返回
        if (vote == null) {
            return;
        }

        // ─── 恢复攻略计数 ───
        Strategy strategy = strategyMapper.selectById(strategyId);
        if (strategy != null) {
            // 根据投票类型恢复对应的计数
            // Math.max(0, ...) 确保计数不会变成负数
            if ("UP".equalsIgnoreCase(vote.getVoteType())) {
                strategy.setUpvotes(Math.max(0, strategy.getUpvotes() - 1));   // 点赞数-1
            } else if ("DOWN".equalsIgnoreCase(vote.getVoteType())) {
                strategy.setDownvotes(Math.max(0, strategy.getDownvotes() - 1)); // 踩数-1
            }
            strategyMapper.updateById(strategy);
        }

        // ─── 删除投票记录 ───
        // DELETE FROM tb_vote WHERE strategy_id = ? AND user_id = ?
        voteMapper.delete(wrapper);
    }

    /**
     * 删除攻略 —— 连同关联数据一起删除
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 当用户想删除自己发布的攻略时，调用这个方法。
     * 它会删除攻略主体以及所有关联数据：
     * 1. 删除攻略-符文关联（tb_strategy_augment）
     * 2. 删除攻略-装备关联（tb_strategy_item）
     * 3. 删除所有投票记录（tb_vote）
     * 4. 删除攻略主体（tb_strategy）
     *
     * ══════════════════════════════════════════════════════════════
     * 权限校验
     * ══════════════════════════════════════════════════════════════
     *
     * 只有攻略的作者才能删除自己的攻略。
     * 如果其他用户尝试删除，会抛出 IllegalArgumentException。
     *
     * 为什么不在 Controller 层做权限校验？
     * - Controller 层主要做参数格式校验
     * - 业务权限校验放在 Service 层更安全（防止绕过 Controller 直接调用 Service）
     *
     * ══════════════════════════════════════════════════════════════
     * 为什么需要 @Transactional？
     * ══════════════════════════════════════════════════════════════
     *
     * 删除攻略涉及多张表的操作，必须保证全部成功或全部回滚。
     * 如果只删除了攻略主体但没删除关联数据，就会出现"孤儿记录"——
     * 关联数据引用了一个不存在的攻略ID，导致数据不一致。
     *
     * ══════════════════════════════════════════════════════════════
     * 删除顺序说明
     * ══════════════════════════════════════════════════════════════
     *
     * 先删关联数据，再删主表数据。为什么？
     * - 如果先删主表，关联数据就变成了"孤儿记录"
     * - 虽然有事务保护（失败会回滚），但先删关联是更规范的做法
     * - 类比：搬家时先搬走小件物品，最后搬大件
     *
     * @param strategyId 攻略ID
     * @param userId     当前用户ID（用于权限校验）
     *
     * @throws IllegalArgumentException 当攻略不存在或用户无权删除时抛出
     */
    @Override
    @Transactional
    public void deleteStrategy(Long strategyId, Long userId) {
        // ─── 记录操作日志 ───
        log.info("Deleting strategy: strategyId={}, userId={}", strategyId, userId);

        // ─── 查询攻略 ───
        Strategy strategy = strategyMapper.selectById(strategyId);
        if (strategy == null) {
            throw new IllegalArgumentException("攻略不存在");
        }

        // ─── 权限校验：只有作者才能删除 ───
        // strategy.getUserId() 是攻略发布者的ID
        // userId 是当前操作者的ID
        // 两者必须相等才允许删除
        if (!strategy.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除他人的攻略");
        }

        // ─── 删除攻略-符文关联 ───
        // DELETE FROM tb_strategy_augment WHERE strategy_id = ?
        strategyAugmentMapper.delete(new LambdaQueryWrapper<StrategyAugment>()
                .eq(StrategyAugment::getStrategyId, strategyId));

        // ─── 删除攻略-装备关联 ───
        // DELETE FROM tb_strategy_item WHERE strategy_id = ?
        strategyItemMapper.delete(new LambdaQueryWrapper<StrategyItem>()
                .eq(StrategyItem::getStrategyId, strategyId));

        // ─── 删除所有投票记录 ───
        // DELETE FROM tb_vote WHERE strategy_id = ?
        voteMapper.delete(new LambdaQueryWrapper<Vote>()
                .eq(Vote::getStrategyId, strategyId));

        // ─── 删除攻略主体 ───
        // DELETE FROM tb_strategy WHERE id = ?
        strategyMapper.deleteById(strategyId);

        log.info("Strategy deleted: strategyId={}", strategyId);
    }

    /**
     * 实体转列表VO —— 将 Strategy 实体转换为 StrategyListVO
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 这是一个私有辅助方法，用于将数据库查询出来的 Strategy 实体对象
     * 转换为前端需要的 StrategyListVO（View Object，视图对象）。
     *
     * ══════════════════════════════════════════════════════════════
     * 为什么需要转换？
     * ══════════════════════════════════════════════════════════════
     *
     * Strategy 是数据库表的直接映射，包含原始的 hero_id、user_id 等外键。
     * 但前端需要的是英雄名称、作者昵称等人类可读的信息。
     * 所以需要额外查询关联表，把ID转换为名称。
     *
     * 类比：
     * - Strategy 好比数据库中的"学号"（如 20230001）
     * - StrategyListVO 好比成绩单上的"姓名"（如 张三）
     *
     * ══════════════════════════════════════════════════════════════
     * VO 与实体的字段对应关系
     * ══════════════════════════════════════════════════════════════
     *
     * Strategy 实体字段        → StrategyListVO 字段     | 说明
     * ─────────────────────────────────────────────────────────────
     * id                       → id                      | 直接复制
     * userId                   → userId                  | 直接复制
     * heroId                   → heroId                  | 直接复制
     * title                    → title                   | 直接复制
     * description              → description             | 直接复制
     * upvotes                  → upvotes                 | 直接复制
     * downvotes                → downvotes               | 直接复制
     * upvotes - downvotes      → score                   | 计算得分
     * createdAt                → createdAt               | 直接复制
     * hero.nameZh（关联查询）  → heroName                | 英雄中文名
     * hero.imageUrl（关联查询）→ heroIcon                | 英雄头像
     * user.nickname（关联查询）→ authorNickname           | 作者昵称
     * user.avatarUrl（关联查询）→ authorAvatar            | 作者头像
     *
     * @param strategy 攻略实体对象（来自数据库查询）
     * @return StrategyListVO 列表视图对象（给前端使用）
     */
    private StrategyListVO convertToListVO(Strategy strategy) {
        StrategyListVO vo = new StrategyListVO();

        // ─── 直接复制字段 ───
        vo.setId(strategy.getId());
        vo.setUserId(strategy.getUserId());
        vo.setHeroId(strategy.getHeroId());
        vo.setTitle(strategy.getTitle());
        vo.setDescription(strategy.getDescription());
        vo.setUpvotes(strategy.getUpvotes());
        vo.setDownvotes(strategy.getDownvotes());

        // ─── 计算得分 ───
        // 得分 = 点赞数 - 踩数
        // 这个值用于前端显示"热度"，正值表示受欢迎，负值表示不受欢迎
        vo.setScore(strategy.getUpvotes() - strategy.getDownvotes());

        vo.setCreatedAt(strategy.getCreatedAt());

        // ─── 查询关联英雄信息 ───
        // 根据 heroId 查询英雄的中文名称和头像URL
        // 如果英雄不存在（被删除了），heroName 和 heroIcon 保持默认值 null
        Hero hero = heroMapper.selectById(strategy.getHeroId());
        if (hero != null) {
            vo.setHeroName(hero.getNameZh());    // 英雄中文名，如"亚索"
            vo.setHeroIcon(hero.getImageUrl());   // 英雄头像URL
        }

        // ─── 查询关联用户信息 ───
        // 根据 userId 查询作者的昵称和头像URL
        // 如果用户不存在（被注销了），authorNickname 和 authorAvatar 保持默认值 null
        User user = userMapper.selectById(strategy.getUserId());
        if (user != null) {
            vo.setAuthorNickname(user.getNickname());   // 作者昵称
            vo.setAuthorAvatar(user.getAvatarUrl());     // 作者头像URL
        }

        return vo;
    }

    /**
     * 实体转详情VO —— 将 Strategy 实体转换为 StrategyDetailVO
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 与 convertToListVO() 类似，但返回的是详情视图对象。
     * 详情VO和列表VO的区别：
     * - 列表VO：用于列表展示，字段较少（性能优先）
     * - 详情VO：用于详情页面，字段更完整
     *
     * ══════════════════════════════════════════════════════════════
     * 为什么列表VO和详情VO要分开？
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 性能考虑：列表页一次显示多条数据，每条数据越精简越好
     * 2. 安全考虑：列表页不需要展示所有字段（比如长文本描述在列表中只显示摘要）
     * 3. 前端需求：列表页和详情页需要的数据格式可能不同
     *
     * 目前两个VO的字段基本相同，但未来详情VO可能会增加：
     * - 符文推荐列表
     * - 装备推荐列表
     * - 当前用户的投票状态
     * 等更多详细信息。
     *
     * @param strategy 攻略实体对象（来自数据库查询）
     * @return StrategyDetailVO 详情视图对象（给前端使用）
     */
    private StrategyDetailVO convertToDetailVO(Strategy strategy) {
        StrategyDetailVO vo = new StrategyDetailVO();

        // ─── 直接复制字段 ───
        vo.setId(strategy.getId());
        vo.setUserId(strategy.getUserId());
        vo.setHeroId(strategy.getHeroId());
        vo.setTitle(strategy.getTitle());
        vo.setDescription(strategy.getDescription());
        vo.setUpvotes(strategy.getUpvotes());
        vo.setDownvotes(strategy.getDownvotes());
        vo.setCreatedAt(strategy.getCreatedAt());

        // ─── 查询关联英雄信息 ───
        Hero hero = heroMapper.selectById(strategy.getHeroId());
        if (hero != null) {
            vo.setHeroName(hero.getNameZh());
            vo.setHeroIcon(hero.getImageUrl());
        }

        // ─── 查询关联用户信息 ───
        User user = userMapper.selectById(strategy.getUserId());
        if (user != null) {
            vo.setAuthorNickname(user.getNickname());
            vo.setAuthorAvatar(user.getAvatarUrl());
        }

        return vo;
    }
}
