package com.aram.mayhem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aram.mayhem.dto.AugmentBriefVO;
import com.aram.mayhem.dto.HeroDetailVO;
import com.aram.mayhem.dto.HeroListVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.common.BusinessException;
import com.aram.mayhem.mapper.AugmentMapper;
import com.aram.mayhem.mapper.HeroMapper;
import com.aram.mayhem.service.HeroService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 英雄服务实现类 —— 英雄模块的"后厨"，真正做菜的地方
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类实现了 HeroService 接口，是英雄模块所有业务逻辑的"真正执行者"。
 * HeroService 接口只定义了"有哪些方法"（菜单），这个类负责"具体怎么做"（做菜）。
 *
 * 它负责两件核心事情：
 * 1. getHeroList()  → 分页查询英雄列表（支持搜索、筛选、排序）
 * 2. getHeroDetail() → 查询单个英雄的详细信息
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、这个类依赖了哪些"帮手"？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 依赖对象              | 作用                           | 打个比方
 * ----------------------|-------------------------------|------------------
 * HeroMapper            | 操作 tb_hero 数据库表          | 仓库管理员（取英雄数据）
 * AugmentMapper         | 操作 tb_augment 数据库表       | 仓库管理员（取符文数据）
 * Logger (log)          | 记录运行日志                   | 工作记录本
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、缓存策略说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本类使用了 Spring Cache 的 @Cacheable 注解来实现缓存：
 *
 * 【英雄列表缓存】
 * - 缓存key：heroList::{page}:{size}:{keyword}:{tier}:{sortBy}
 * - 缓存条件：结果不为null且records不为空时才缓存
 * - 效果：相同查询条件的重复请求直接从Redis返回，不查数据库
 *
 * 【英雄详情缓存】
 * - 缓存key：heroDetail::{英雄ID}
 * - 缓存条件：结果不为null时才缓存
 * - 效果：相同ID的详情查询直接从Redis返回
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、数据流转图
 * ═══════════════════════════════════════════════════════════════════
 *
 * 用户请求
 *    │
 *    ▼
 * HeroController（接收HTTP请求）
 *    │
 *    ▼ 调用
 * HeroServiceImpl（本类，执行业务逻辑）
 *    │
 *    ├──► 先查Redis缓存（@Cacheable自动处理）
 *    │    └── 缓存命中 → 直接返回，不查数据库
 *    │
 *    └──► 缓存未命中 → 查数据库
 *         │
 *         ├──► HeroMapper.selectPage()（查英雄列表）
 *         ├──► HeroMapper.selectById()（查英雄详情）
 *         └──► AugmentMapper.selectList()（查关联符文）
 *              │
 *              ▼
 *         Entity → VO 转换（convertToListVO / convertToDetailVO）
 *              │
 *              ▼
 *         返回给Controller → 同时写入Redis缓存
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、关键概念解释
 * ═══════════════════════════════════════════════════════════════════
 *
 * 【Entity vs VO】
 * - Entity（实体类）：和数据库表一一对应，如 Hero 类对应 tb_hero 表
 * - VO（View Object）：给前端展示用的数据对象，只包含前端需要的字段
 * - 为什么要转换？因为数据库字段和前端需要的字段不一定完全一样
 *   例如：数据库有 created_at、updated_at，但前端列表不需要这些字段
 *
 * 【LambdaQueryWrapper】
 * - MyBatis-Plus 提供的查询条件构造器
 * - 用 Lambda 表达式引用字段名，编译时就能检查字段是否存在
 * - 例如：wrapper.eq(Hero::getTier, "S+") 会生成 WHERE tier = 'S+'
 *
 * 【Page 分页】
 * - MyBatis-Plus 提供的分页对象
 * - new Page<>(1, 10) 表示第1页，每页10条
 * - selectPage() 会自动执行 COUNT 查询获取总数 + LIMIT 查询获取当前页数据
 *
 * @see com.aram.mayhem.service.HeroService 英雄服务接口（菜单）
 * @see com.aram.mayhem.entity.Hero 英雄实体类（数据库表映射）
 * @see com.aram.mayhem.mapper.HeroMapper 英雄数据访问层
 */
@Service
public class HeroServiceImpl implements HeroService {

    private static final Logger log = LoggerFactory.getLogger(HeroServiceImpl.class);

    private final HeroMapper heroMapper;

    private final AugmentMapper augmentMapper;

    /**
     * 构造函数 —— Spring 自动注入依赖对象
     *
     * ═══════════════════════════════════════════════════════════════
     * 什么是构造函数注入？
     * ═══════════════════════════════════════════════════════════════
     *
     * 当 Spring 创建 HeroServiceImpl 对象时，会自动找到
     * HeroMapper 和 AugmentMapper 的实现类，传给这个构造函数。
     *
     * 打个比方：
     * - 你去餐厅上班，餐厅会自动给你配好"仓库钥匙"（HeroMapper）
     *   和"调料柜钥匙"（AugmentMapper），你不需要自己去拿
     *
     * 为什么要用构造函数注入而不是 @Autowired 字段注入？
     * 1. 依赖不可变：final 字段只能在构造函数中赋值，之后不能被修改
     * 2. 依赖明确：看构造函数就知道这个类需要哪些依赖
     * 3. 方便测试：单元测试时可以直接 new 对象传入 Mock 依赖
     *
     * @param heroMapper    英雄数据访问层，用于查询 tb_hero 表
     * @param augmentMapper 符文数据访问层，用于查询 tb_augment 表（英雄详情需要关联符文）
     */
    public HeroServiceImpl(HeroMapper heroMapper, AugmentMapper augmentMapper) {
        this.heroMapper = heroMapper;
        this.augmentMapper = augmentMapper;
    }

    /**
     * 获取英雄列表 —— 英雄模块的核心查询方法
     *
     * ═══════════════════════════════════════════════════════════════
     * 功能说明
     * ═══════════════════════════════════════════════════════════════
     *
     * 分页查询英雄列表，支持关键词搜索、梯级筛选和多种排序方式。
     * 查询结果会被自动缓存到 Redis。
     *
     * ═══════════════════════════════════════════════════════════════
     * 执行流程（一步步看这个方法做了什么）
     * ═══════════════════════════════════════════════════════════════
     *
     * 第1步：构建查询条件（LambdaQueryWrapper）
     *   ├── 如果有 keyword → 添加搜索条件（英文名 OR 中文名 OR 称号 LIKE 关键词）
     *   ├── 如果有 tier   → 添加梯级筛选条件（tier = 指定梯级）
     *   └── 根据 sortBy   → 添加排序条件（按胜率/选取率/梯级/名称排序）
     *
     * 第2步：执行分页查询
     *   └── heroMapper.selectPage(page, wrapper) → 返回 Page<Hero>
     *
     * 第3步：Entity → VO 转换
     *   └── 将 Hero 实体列表转换为 HeroListVO 列表（只保留前端需要的字段）
     *
     * 第4步：封装分页结果
     *   └── 返回 PageResult<HeroListVO>（包含总数、页码、每页数量、当前页数据）
     *
     * ═══════════════════════════════════════════════════════════════
     * 缓存说明
     * ═══════════════════════════════════════════════════════════════
     *
     * @Cacheable 注解会让 Spring 在方法执行前后自动处理缓存：
     * - 执行前：先检查 Redis 中是否有缓存（key = 拼接的查询条件）
     * - 缓存命中：直接返回缓存数据，不执行方法体
     * - 缓存未命中：执行方法体，将返回值存入 Redis
     *
     * key 的拼接规则：
     * "1:10:::winRate" 表示 page=1, size=10, keyword=null, tier=null, sortBy=winRate
     *
     * unless 条件：如果结果为null或records为空列表，不缓存
     * （避免缓存空结果，浪费Redis空间）
     *
     * @param page    页码，从1开始（第1页、第2页...）
     * @param size    每页显示的英雄数量
     * @param keyword 搜索关键词（可选，null表示不搜索）
     * @param tier    梯级筛选（可选，null表示不筛选）
     * @param sortBy  排序字段（可选，null默认按胜率排序）
     * @return PageResult<HeroListVO> 分页英雄列表结果
     */
    @Override
    @Cacheable(value = "heroList", key = "#page + ':' + #size + ':' + (#keyword != null ? #keyword : '') + ':' + (#tier != null ? #tier : '') + ':' + (#sortBy != null ? #sortBy : '')", unless = "#result == null || #result.records.isEmpty()")
    public PageResult<HeroListVO> getHeroList(int page, int size, String keyword, String tier, String sortBy) {
        // 创建查询条件构造器 —— 相当于 SQL 的 WHERE 子句构建器
        // LambdaQueryWrapper<Hero> 表示我们要查询的是 Hero 表
        LambdaQueryWrapper<Hero> queryWrapper = new LambdaQueryWrapper<>();

        // ── 关键词搜索 ──
        // StringUtils.hasText() 检查字符串是否非null、非空、非纯空格
        if (StringUtils.hasText(keyword)) {
            // .and() 表示把多个条件用括号包起来，形成 (条件1 OR 条件2 OR 条件3)
            // 这样可以避免和其他条件（如tier筛选）产生错误的AND/OR优先级
            queryWrapper.and(wrapper -> wrapper
                    // LIKE 模糊查询：WHERE name_en LIKE '%盖伦%'
                    .like(Hero::getNameEn, keyword)
                    .or()  // OR 连接，三个字段只要有一个匹配就算命中
                    .like(Hero::getNameZh, keyword)
                    .or()
                    .like(Hero::getTitle, keyword));
        }

        // ── 梯级筛选 ──
        // 精确匹配：WHERE tier = 'S+'
        if (StringUtils.hasText(tier)) {
            queryWrapper.eq(Hero::getTier, tier);
        }

        // ── 排序逻辑 ──
        // switch 表达式（Java 14+语法），根据 sortBy 参数选择排序字段
        if (StringUtils.hasText(sortBy)) {
            switch (sortBy) {
                // orderByDesc = 降序排列（从大到小）
                // 胜率高的排前面，因为用户最关心强势英雄
                case "winRate" -> queryWrapper.orderByDesc(Hero::getWinRate);
                // 选取率高的排前面，看哪些英雄最受欢迎
                case "pickRate" -> queryWrapper.orderByDesc(Hero::getPickRate);
                // orderByAsc = 升序排列（从小到大）
                // 梯级用升序是因为 S+ < S < A < B < C（字母顺序）
                case "tier" -> queryWrapper.orderByAsc(Hero::getTier);
                // 英文名升序，按A-Z排列
                case "name" -> queryWrapper.orderByAsc(Hero::getNameEn);
                // 其他未识别的排序字段，默认按胜率降序
                default -> queryWrapper.orderByDesc(Hero::getWinRate);
            }
        } else {
            // 没有指定排序字段时，默认按胜率降序排列
            queryWrapper.orderByDesc(Hero::getWinRate);
        }

        // 创建分页对象：第page页，每页size条
        // MyBatis-Plus 会自动生成 LIMIT 语句
        Page<Hero> heroPage = new Page<>(page, size);

        // 执行查询：将查询条件和分页参数传给 Mapper
        // 返回的 result 包含：总记录数、当前页数据列表、分页信息等
        Page<Hero> result = heroMapper.selectPage(heroPage, queryWrapper);

        // ── Entity → VO 转换 ──
        // result.getRecords() 获取当前页的 Hero 实体列表
        // .stream().map(this::convertToListVO) 将每个 Hero 转换为 HeroListVO
        // .toList() 收集为不可变列表（Java 16+语法）
        List<HeroListVO> records = result.getRecords().stream()
                .map(this::convertToListVO)
                .toList();

        // 封装分页结果返回
        // result.getTotal()    → 符合条件的英雄总数（用于前端计算总页数）
        // result.getCurrent()  → 当前页码
        // result.getSize()     → 每页数量
        // records              → 当前页的英雄VO列表
        return new PageResult<>(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), records);
    }

    /**
     * 获取英雄详情 —— 查看单个英雄的完整信息
     *
     * ═══════════════════════════════════════════════════════════════
     * 功能说明
     * ═══════════════════════════════════════════════════════════════
     *
     * 根据英雄ID获取该英雄的详细信息，包括：
     * - 基本信息（名称、称号、角色、梯级、胜率等）
     * - 战斗数据（平均击杀/死亡/助攻）
     * - 推荐出装
     * - 推荐符文（通过 resolveAugmentBriefs 方法关联查询）
     * - 技能列表（被动 + Q/W/E/R）
     * - 克制技巧和协同英雄
     *
     * ═══════════════════════════════════════════════════════════════
     * 执行流程
     * ═══════════════════════════════════════════════════════════════
     *
     * 第1步：根据ID查询数据库
     *   └── heroMapper.selectById(id) → 返回 Hero 实体
     *
     * 第2步：检查英雄是否存在
     *   └── 如果返回null → 抛出 BusinessException(404)
     *
     * 第3步：Entity → VO 转换
     *   └── convertToDetailVO(hero) → 返回 HeroDetailVO
     *
     * ═══════════════════════════════════════════════════════════════
     * 缓存说明
     * ═══════════════════════════════════════════════════════════════
     *
     * @Cacheable(value = "heroDetail", key = "#id")
     * - 缓存key：heroDetail::{英雄ID}
     * - 例如：heroDetail::1 缓存ID为1的英雄详情
     * - unless = "#result == null"：如果英雄不存在（返回null），不缓存
     *
     * @param id 英雄的唯一ID（数据库主键）
     * @return HeroDetailVO 英雄详情视图对象
     * @throws BusinessException 英雄不存在时抛出404错误
     */
    @Override
    @Cacheable(value = "heroDetail", key = "#id", unless = "#result == null")
    public HeroDetailVO getHeroDetail(Long id) {
        // 记录查询日志，方便排查问题
        log.info("Getting hero detail: id={}", id);

        // 根据ID查询数据库，返回 Hero 实体对象
        // selectById 是 MyBatis-Plus 提供的方法，等价于 SELECT * FROM tb_hero WHERE id = ?
        Hero hero = heroMapper.selectById(id);

        // 如果查询结果为null，说明这个ID对应的英雄不存在
        if (hero == null) {
            // 记录警告日志
            log.warn("Hero not found: id={}", id);
            // 抛出业务异常，HTTP状态码404，前端会显示"英雄不存在"
            throw new BusinessException(404, "Hero not found with id: " + id);
        }

        // 将 Hero 实体转换为 HeroDetailVO 视图对象
        return convertToDetailVO(hero);
    }

    /**
     * Hero Entity → HeroListVO 转换（私有方法）
     *
     * ═══════════════════════════════════════════════════════════════
     * 为什么要转换？
     * ═══════════════════════════════════════════════════════════════
     *
     * Hero 实体类包含数据库表的所有字段（包括 description、skills 等），
     * 但英雄列表页面只需要展示基本信息（名称、梯级、胜率等），
     * 所以只把需要的字段复制到 HeroListVO 中，减少数据传输量。
     *
     * 打个比方：
     * - Hero 实体 = 一个人完整的档案（姓名、年龄、住址、电话、身份证号...）
     * - HeroListVO = 通讯录里的一条记录（只保留姓名和电话）
     *
     * @param hero 数据库查询出来的 Hero 实体对象
     * @return HeroListVO 只包含列表页所需字段的视图对象
     */
    private HeroListVO convertToListVO(Hero hero) {
        HeroListVO vo = new HeroListVO();
        // 逐个字段复制，从 Hero 实体到 HeroListVO
        // 这里不能用 BeanUtils.copyProperties，因为两个类的字段不完全一样
        vo.setId(hero.getId());                           // 英雄唯一ID
        vo.setNameEn(hero.getNameEn());                   // 英文名（如 Garen）
        vo.setNameZh(hero.getNameZh());                   // 中文名（如 盖伦）
        vo.setTitle(hero.getTitle());                      // 称号（如 德玛西亚之力）
        vo.setRole(hero.getRole());                        // 角色定位（如 Tank、Mage）
        vo.setTier(hero.getTier());                        // 梯级（S+/S/A/B/C）
        vo.setWinRate(hero.getWinRate());                  // 胜率（如 52.35）
        vo.setPickRate(hero.getPickRate());                // 选取率（如 8.12）
        vo.setImageUrl(hero.getImageUrl());                // 头像图片URL
        vo.setIsVersionTrap(hero.getIsVersionTrap());      // 是否为版本陷阱（看起来强但实际弱）
        return vo;
    }

    /**
     * Hero Entity → HeroDetailVO 转换（私有方法）
     *
     * ═══════════════════════════════════════════════════════════════
     * 功能说明
     * ═══════════════════════════════════════════════════════════════
     *
     * 将 Hero 实体转换为详情页所需的 HeroDetailVO 对象。
     * 详情页比列表页需要更多字段，包括战斗数据、技能、推荐出装等。
     *
     * 特别注意：
     * - recommendedAugments 需要通过 resolveAugmentBriefs() 方法
     *   从符文ID列表关联查询符文详细信息
     * - skills 需要从 JSON 格式转换为 SkillInfo 列表
     * - counterTips 和 synergies 可能为null，需要设置空列表作为默认值
     *
     * @param hero 数据库查询出来的 Hero 实体对象
     * @return HeroDetailVO 包含完整英雄详情的视图对象
     */
    private HeroDetailVO convertToDetailVO(Hero hero) {
        HeroDetailVO vo = new HeroDetailVO();
        // ── 基本信息字段（和列表VO相同）──
        vo.setId(hero.getId());
        vo.setNameEn(hero.getNameEn());
        vo.setNameZh(hero.getNameZh());
        vo.setTitle(hero.getTitle());
        vo.setRole(hero.getRole());
        vo.setTier(hero.getTier());
        vo.setWinRate(hero.getWinRate());
        vo.setPickRate(hero.getPickRate());
        vo.setImageUrl(hero.getImageUrl());
        vo.setIsVersionTrap(hero.getIsVersionTrap());

        // ── 详情页额外字段 ──
        vo.setDescription(hero.getDescription());          // 英雄描述文本
        vo.setAvgKills(hero.getAvgKills());                // 平均击杀数
        vo.setAvgDeaths(hero.getAvgDeaths());              // 平均死亡数
        vo.setAvgAssists(hero.getAvgAssists());            // 平均助攻数
        vo.setRecommendedBuild(hero.getRecommendedBuild()); // 推荐出装（JSON字符串）

        // ── 推荐符文关联查询 ──
        // hero.getRecommendedAugmentIds() 返回推荐符文的ID列表（如 [1, 5, 8]）
        // resolveAugmentBriefs() 会根据这些ID去数据库查询符文的名称、品质、图标
        vo.setRecommendedAugments(resolveAugmentBriefs(hero.getRecommendedAugmentIds()));

        // ── 技能列表转换 ──
        // hero.getSkills() 返回 Hero.SkillData 列表（数据库中存储为JSON）
        // 需要转换为 HeroDetailVO.SkillInfo 列表（前端展示格式）
        if (hero.getSkills() != null) {
            List<HeroDetailVO.SkillInfo> skillInfos = hero.getSkills().stream()
                    .map(skill -> {
                        HeroDetailVO.SkillInfo info = new HeroDetailVO.SkillInfo();
                        info.setKey(skill.getKey());              // 技能按键（被动/Q/W/E/R）
                        info.setName(skill.getName());            // 技能名称
                        info.setDescription(skill.getDescription()); // 技能描述
                        return info;
                    })
                    .toList();
            vo.setSkills(skillInfos);
        } else {
            // 技能数据为null时，设置空列表，避免前端报NullPointerException
            vo.setSkills(List.of());
        }

        // ── 克制技巧和协同英雄 ──
        // 使用三元运算符处理null值：有数据就用数据，没有就返回空列表
        vo.setCounterTips(hero.getCounterTips() != null ? hero.getCounterTips() : List.of());
        vo.setSynergies(hero.getSynergies() != null ? hero.getSynergies() : List.of());

        return vo;
    }

    /**
     * 根据符文ID列表查询符文简要信息（私有方法）
     *
     * ═══════════════════════════════════════════════════════════════
     * 功能说明
     * ═══════════════════════════════════════════════════════════════
     *
     * 英雄详情页需要展示"推荐符文"，但 Hero 表只存储了符文ID列表，
     * 不存储符文的名称、图标等信息。所以需要根据ID去 Augment 表查询。
     *
     * ═══════════════════════════════════════════════════════════════
     * 什么是 N+1 问题？为什么这里要避免？
     * ═══════════════════════════════════════════════════════════════
     *
     * N+1 问题：如果对每个符文ID都执行一次 SELECT 查询，
     * 有N个符文就要执行N+1次查询（1次查英雄 + N次查符文），非常慢。
     *
     * 解决方案：使用 IN 查询，一次性获取所有符文数据。
     * SELECT * FROM tb_augment WHERE id IN (1, 5, 8)
     * 只需1次查询就能获取所有符文，效率大大提高。
     *
     * ═══════════════════════════════════════════════════════════════
     * 执行流程
     * ═══════════════════════════════════════════════════════════════
     *
     * 第1步：空值检查 → augmentIds 为null或空列表时直接返回空列表
     * 第2步：IN 查询 → 一次性从数据库获取所有符文实体
     * 第3步：构建Map → 以ID为key，方便后续按ID查找
     * 第4步：按原始顺序遍历 → 保持推荐符文的顺序不变
     * 第5步：过滤null → 如果某个ID在数据库中不存在，跳过它
     * 第6步：转换为VO → 只保留前端需要的字段（名称、品质、图标）
     *
     * @param augmentIds 符文ID列表（如 [1, 5, 8]）
     * @return List<AugmentBriefVO> 符文简要信息列表
     */
    private List<AugmentBriefVO> resolveAugmentBriefs(List<Long> augmentIds) {
        // 空值检查：如果没有推荐符文，直接返回空列表
        if (augmentIds == null || augmentIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 使用 IN 查询批量获取所有符文实体
        // 生成的SQL：SELECT * FROM tb_augment WHERE id IN (1, 5, 8)
        // 比逐个查询高效得多
        List<Augment> augments = augmentMapper.selectList(
                new LambdaQueryWrapper<Augment>().in(Augment::getId, augmentIds));

        // 将 List<Augment> 转换为 Map<Long, Augment>
        // key = 符文ID，value = 符文实体
        // 这样后续可以通过 ID 快速查找符文，时间复杂度 O(1)
        Map<Long, Augment> augmentMap = augments.stream()
                .collect(Collectors.toMap(Augment::getId, a -> a));

        // 按原始 augmentIds 的顺序遍历，保持推荐符文的顺序
        // .map(augmentMap::get) → 根据ID从Map中获取对应的Augment实体
        // .filter(Objects::nonNull) → 过滤掉数据库中不存在的符文ID
        // .map(a -> new AugmentBriefVO(...)) → 转换为简要信息VO
        return augmentIds.stream()
                .map(augmentMap::get)
                .filter(java.util.Objects::nonNull)
                .map(a -> new AugmentBriefVO(a.getId(), a.getNameZh(), a.getQuality(), a.getIconUrl()))
                .toList();
    }
}
