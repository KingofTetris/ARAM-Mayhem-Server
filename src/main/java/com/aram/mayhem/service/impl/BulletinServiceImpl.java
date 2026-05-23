package com.aram.mayhem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aram.mayhem.common.BusinessException;
import com.aram.mayhem.dto.BulletinDetailVO;
import com.aram.mayhem.dto.BulletinListVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.entity.Bulletin;
import com.aram.mayhem.mapper.BulletinMapper;
import com.aram.mayhem.service.BulletinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 公告服务实现类 —— 公告模块的"后厨"，真正做菜的地方
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类实现了 BulletinService 接口，是公告模块所有业务逻辑的"真正执行者"。
 * BulletinService 接口只定义了"有哪些方法"（菜单），这个类负责"具体怎么做"（做菜）。
 *
 * 公告模块用于向用户展示系统通知、版本更新、活动信息等，包括：
 * - 分页查询公告列表（支持按类型筛选）
 * - 获取最新公告（首页轮播展示）
 * - 查看公告详情
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、这个类依赖了哪些"帮手"？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 依赖对象              | 作用                           | 打个比方
 * ----------------------|-------------------------------|------------------
 * BulletinMapper        | 操作 tb_bulletin 数据库表      | 仓库管理员（取公告数据）
 * Logger (log)          | 记录运行日志                   | 工作记录本
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、公告类型说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 公告分为三种类型，存储在 tb_bulletin 表的 type 字段中：
 *
 * 类型值      | 含义        | 使用场景
 * ────────────────────────────────────────────────────────
 * version    | 版本更新    | 新版本发布、功能更新说明
 * event      | 活动公告    | 限时活动、赛事通知
 * notice     | 系统公告    | 维护通知、规则变更
 *
 * 用户可以通过 type 参数筛选特定类型的公告，也可以不传 type 查看所有类型。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、置顶排序规则说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 公告列表的排序规则是：置顶公告优先，然后按发布时间降序。
 *
 * 排序逻辑：
 * 1. 第一排序：is_pinned DESC（置顶的排前面）
 * 2. 第二排序：created_at DESC（新的排前面）
 *
 * 举例：
 * ┌──────────────────────────────────────────────┐
 * │ [置顶] v2.0 版本更新公告（2026-05-20）       │  ← is_pinned=1，排最前
 * │ [置顶] 五一活动公告（2026-05-01）            │  ← is_pinned=1，按时间排
 * │ 普通公告 A（2026-05-18）                     │  ← is_pinned=0，按时间排
 * │ 普通公告 B（2026-05-15）                     │  ← is_pinned=0，按时间排
 * └──────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、方法总览
 * ═══════════════════════════════════════════════════════════════════
 *
 * 方法名                  | 功能                        | 是否事务
 * ------------------------|----------------------------|----------
 * getBulletinList()       | 分页查询公告列表（含筛选）  | 否
 * getLatestBulletins()    | 获取最新N条公告             | 否
 * getBulletinDetail()     | 查看公告详情                | 否
 * convertToListVO()       | 实体→列表VO转换（私有）     | 否
 * convertToDetailVO()     | 实体→详情VO转换（私有）     | 否
 */
@Service
public class BulletinServiceImpl implements BulletinService {

    /**
     * 日志记录器 —— 用来记录程序运行过程中的关键信息
     *
     * 本类主要使用 INFO 和 WARN 级别：
     * - INFO：记录正常的查询操作
     * - WARN：记录公告不存在的异常情况
     */
    private static final Logger log = LoggerFactory.getLogger(BulletinServiceImpl.class);

    /**
     * 公告数据访问对象 —— 负责与 tb_bulletin 表交互
     *
     * 主要操作：
     * - selectPage()     → 分页查询公告列表
     * - selectList()     → 查询公告列表（不分页）
     * - selectById()     → 根据ID查询单条公告
     *
     * 对应数据库表：tb_bulletin
     * 主要字段：id, type, title, content, image_url, is_pinned, published_at, created_at
     */
    private final BulletinMapper bulletinMapper;

    /**
     * 构造函数 —— Spring 自动注入所有依赖
     *
     * @param bulletinMapper 公告数据访问对象
     */
    public BulletinServiceImpl(BulletinMapper bulletinMapper) {
        this.bulletinMapper = bulletinMapper;
    }

    /**
     * 获取公告列表 —— 公告模块的核心浏览功能
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 当用户打开公告页面时，需要展示一个公告列表。
     * 这个方法负责从数据库中分页查询公告数据，支持按类型筛选。
     *
     * ══════════════════════════════════════════════════════════════
     * 参数说明
     * ══════════════════════════════════════════════════════════════
     *
     * @param type 公告类型（可选），支持三种值：
     *             - "version" → 只查版本更新公告
     *             - "event"   → 只查活动公告
     *             - "notice"  → 只查系统公告
     *             - null/空   → 查询所有类型
     *
     * @param page 页码，从1开始
     * @param size 每页显示的公告数量
     *
     * @return PageResult<BulletinListVO> 分页结果，包含：
     *         - total：总记录数
     *         - current：当前页码
     *         - size：每页条数
     *         - records：当前页的公告列表
     *
     * ══════════════════════════════════════════════════════════════
     * 执行流程
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 构建查询条件 → 如果有 type 则添加类型筛选
     * 2. 设置排序规则 → 置顶优先 + 时间降序
     * 3. 执行分页查询 → 从数据库获取数据
     * 4. 转换为VO → 将实体转换为视图对象
     * 5. 封装分页结果 → 返回 PageResult
     *
     * ══════════════════════════════════════════════════════════════
     * PageResult vs List 的区别
     * ══════════════════════════════════════════════════════════════
     *
     * 与 StrategyServiceImpl 的 getStrategyList() 返回 List 不同，
     * 这个方法返回 PageResult，包含了分页元数据（总数、页码等）。
     * 前端需要这些信息来渲染分页组件（如"第1页/共5页"）。
     */
    @Override
    public PageResult<BulletinListVO> getBulletinList(String type, int page, int size) {
        // ─── 记录操作日志 ───
        log.info("Getting bulletin list: type={}, page={}, size={}", type, page, size);

        // ─── 构建查询条件 ───
        LambdaQueryWrapper<Bulletin> queryWrapper = new LambdaQueryWrapper<>();

        // ─── 类型筛选 ───
        // StringUtils.hasText() 检查字符串是否非空且非纯空格
        // 如果 type 有值，添加 WHERE type = ? 条件
        if (StringUtils.hasText(type)) {
            queryWrapper.eq(Bulletin::getType, type);
        }

        // ─── 排序规则 ───
        // 第一排序：is_pinned DESC（置顶的排前面，1 > 0）
        // 第二排序：created_at DESC（新的排前面）
        // 生成的 SQL：ORDER BY is_pinned DESC, created_at DESC
        queryWrapper.orderByDesc(Bulletin::getIsPinned)
                .orderByDesc(Bulletin::getCreatedAt);

        // ─── 执行分页查询 ───
        Page<Bulletin> bulletinPage = new Page<>(page, size);
        Page<Bulletin> result = bulletinMapper.selectPage(bulletinPage, queryWrapper);

        // ─── 转换为VO ───
        // 将 Page<Bulletin> 中的记录列表转换为 List<BulletinListVO>
        // .toList() 是 Java 16+ 的简化写法，等价于 .collect(Collectors.toList())
        List<BulletinListVO> records = result.getRecords().stream()
                .map(this::convertToListVO)
                .toList();

        // ─── 封装分页结果 ───
        // PageResult 包含：总数、当前页码、每页条数、数据列表
        return new PageResult<>(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), records);
    }

    /**
     * 获取最新公告 —— 首页轮播展示功能
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 首页需要展示最新的几条公告，用于轮播或通知栏展示。
     * 这个方法查询最新的 N 条公告，同样遵循置顶优先的排序规则。
     *
     * 与 getBulletinList() 的区别：
     * - getBulletinList() 返回分页结果，适合公告列表页
     * - getLatestBulletins() 返回固定数量的最新公告，适合首页展示
     *
     * @param limit 获取数量，默认3条
     * @return List<BulletinListVO> 最新公告列表
     *
     * ══════════════════════════════════════════════════════════════
     * ⚠️ SQL 注入风险说明
     * ══════════════════════════════════════════════════════════════
     *
     * .last("LIMIT " + limit) 直接拼接 SQL 片段，存在 SQL 注入风险。
     * 目前 limit 是 int 类型，不会被注入恶意 SQL，但如果未来改为 String 类型，
     * 必须使用参数化查询替代 .last() 方法。
     *
     * 更安全的替代方案：
     * - 使用 Page 对象设置分页参数（page=1, size=limit）
     * - 或使用 MyBatis-Plus 的 Page 设定
     */
    @Override
    public List<BulletinListVO> getLatestBulletins(int limit) {
        // ─── 记录操作日志 ───
        log.info("Getting latest bulletins: limit={}", limit);

        // ─── 构建查询条件 ───
        LambdaQueryWrapper<Bulletin> queryWrapper = new LambdaQueryWrapper<>();

        // ─── 排序规则（与列表相同） ───
        queryWrapper.orderByDesc(Bulletin::getIsPinned)
                .orderByDesc(Bulletin::getCreatedAt);

        // ─── 限制返回数量 ───
        // .last() 方法会在 SQL 末尾追加原始 SQL 片段
        // 生成 SQL：... ORDER BY is_pinned DESC, created_at DESC LIMIT 3
        // 注意：.last() 不会进行参数化处理，直接拼接字符串
        queryWrapper.last("LIMIT " + limit);

        // ─── 执行查询并转换 ───
        List<Bulletin> bulletins = bulletinMapper.selectList(queryWrapper);
        return bulletins.stream()
                .map(this::convertToListVO)
                .toList();
    }

    /**
     * 获取公告详情 —— 查看公告的完整内容
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 当用户在公告列表中点击某条公告时，需要展示该公告的详细内容。
     * 这个方法根据公告ID查询完整的公告数据。
     *
     * @param id 公告ID，对应数据库中的主键
     * @return BulletinDetailVO 公告详情对象
     *
     * @throws BusinessException 当公告不存在时抛出（HTTP 404）
     *
     * ══════════════════════════════════════════════════════════════
     * 列表VO与详情VO的区别
     * ══════════════════════════════════════════════════════════════
     *
     * 目前 BulletinListVO 和 BulletinDetailVO 的字段基本相同，
     * 但分开定义是为了未来扩展：
     * - 列表VO可能只显示摘要，不显示完整内容
     * - 详情VO可能增加阅读量、评论数等额外信息
     */
    @Override
    public BulletinDetailVO getBulletinDetail(Long id) {
        // ─── 记录操作日志 ───
        log.info("Getting bulletin detail: id={}", id);

        // ─── 根据ID查询公告 ───
        // selectById() 生成 SQL：SELECT * FROM tb_bulletin WHERE id = ?
        Bulletin bulletin = bulletinMapper.selectById(id);
        if (bulletin == null) {
            // 公告不存在，记录警告并抛出业务异常
            log.warn("Bulletin not found: id={}", id);
            throw new BusinessException(404, "Bulletin not found with id: " + id);
        }

        // ─── 转换为详情VO ───
        return convertToDetailVO(bulletin);
    }

    /**
     * 实体转列表VO —— 将 Bulletin 实体转换为 BulletinListVO
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 这是一个私有辅助方法，用于将数据库查询出来的 Bulletin 实体对象
     * 转换为前端需要的 BulletinListVO（View Object，视图对象）。
     *
     * @param bulletin 公告实体对象（来自数据库查询）
     * @return BulletinListVO 列表视图对象（给前端使用）
     */
    private BulletinListVO convertToListVO(Bulletin bulletin) {
        BulletinListVO vo = new BulletinListVO();
        vo.setId(bulletin.getId());                           // 公告ID
        vo.setType(bulletin.getType());                       // 公告类型（version/event/notice）
        vo.setTitle(bulletin.getTitle());                     // 公告标题
        vo.setContent(bulletin.getContent());                 // 公告内容
        vo.setImageUrl(bulletin.getImageUrl());               // 配图URL
        vo.setIsPinned(bulletin.getIsPinned());               // 是否置顶（0/1）
        vo.setPublishedAt(bulletin.getPublishedAt());         // 发布时间
        vo.setCreatedAt(bulletin.getCreatedAt());             // 创建时间
        return vo;
    }

    /**
     * 实体转详情VO —— 将 Bulletin 实体转换为 BulletinDetailVO
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 与 convertToListVO() 类似，但返回的是详情视图对象。
     * 目前字段相同，但未来详情VO可能会增加更多字段。
     *
     * @param bulletin 公告实体对象（来自数据库查询）
     * @return BulletinDetailVO 详情视图对象（给前端使用）
     */
    private BulletinDetailVO convertToDetailVO(Bulletin bulletin) {
        BulletinDetailVO vo = new BulletinDetailVO();
        vo.setId(bulletin.getId());                           // 公告ID
        vo.setType(bulletin.getType());                       // 公告类型
        vo.setTitle(bulletin.getTitle());                     // 公告标题
        vo.setContent(bulletin.getContent());                 // 公告内容全文
        vo.setImageUrl(bulletin.getImageUrl());               // 配图URL
        vo.setIsPinned(bulletin.getIsPinned());               // 是否置顶
        vo.setPublishedAt(bulletin.getPublishedAt());         // 发布时间
        vo.setCreatedAt(bulletin.getCreatedAt());             // 创建时间
        return vo;
    }
}
