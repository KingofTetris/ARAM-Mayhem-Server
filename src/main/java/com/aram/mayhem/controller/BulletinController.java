package com.aram.mayhem.controller;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.dto.BulletinDetailVO;
import com.aram.mayhem.dto.BulletinListVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.service.BulletinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告控制器 —— 管理游戏公告的 REST API 入口，提供公告列表、最新公告、公告详情查询功能
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类是公告模块的 REST API 入口，负责处理前端发来的公告相关请求：
 * 1. getBulletinList()     → 分页查询公告列表（支持按类型筛选）
 * 2. getLatestBulletins()  → 获取最新N条公告（首页轮播用）
 * 3. getBulletinDetail()   → 获取单条公告的详细内容
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、权限说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 所有接口均为公开访问，不需要登录。
 * 原因：公告是面向所有用户的信息发布功能，包括未登录用户也能查看。
 * 在 SecurityConfig 中配置了 /api/bulletins/** → permitAll()
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、公告类型说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * type 字段用于区分公告类别：
 * - "version"：版本更新公告（如"3.15 版本更新说明"）
 * - "event"：活动公告（如"冰雪节限时活动"）
 * - "notice"：普通通知（如"服务器维护通知"）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、排序规则
 * ═══════════════════════════════════════════════════════════════════
 *
 * 公告列表的排序规则：
 * 1. 置顶公告优先显示（isPinned=true 的排前面）
 * 2. 同级别内按发布时间降序（最新的排前面）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * 前端公告页面 → HTTP 请求 → BulletinController → BulletinService → BulletinMapper → MySQL
 *                                                          ↓
 *                                                     Redis 缓存
 *
 * 关联类：
 * - BulletinService：公告业务逻辑层，处理查询、缓存等逻辑
 * - BulletinListVO：公告列表视图对象（不含正文内容，减少传输量）
 * - BulletinDetailVO：公告详情视图对象（含完整正文内容）
 * - PageResult：通用分页结果包装类
 */
@Tag(name = "Bulletin", description = "公告管理接口")
@RestController
@RequestMapping("/api/bulletins")
public class BulletinController {

    private final BulletinService bulletinService;

    /**
     * 依赖注入：公告业务逻辑服务
     *
     * 通过构造函数注入 BulletinService，Spring 会自动找到对应的 Bean 注入。
     * 使用构造函数注入而非 @Autowired 字段注入，因为：
     * 1. 构造函数注入是 Spring 官方推荐的方式
     * 2. 可以将字段声明为 final，确保不可变
     * 3. 便于单元测试时 Mock 依赖
     */
    public BulletinController(BulletinService bulletinService) {
        this.bulletinService = bulletinService;
    }

    /**
     * 获取公告列表（分页 + 类型筛选）—— 公告模块的核心列表接口
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：GET（查询操作用 GET，符合 RESTful 规范）
     * 路径：/api/bulletins
     * 权限：公开访问（不需要登录）
     *
     * ═══════════════════════════════════════════════════════════════════
     * 请求参数说明
     * ═══════════════════════════════════════════════════════════════════
     *
     * type（可选）：公告类型筛选
     *   - 不传：返回所有类型的公告
     *   - "version"：只返回版本更新公告
     *   - "event"：只返回活动公告
     *   - "notice"：只返回普通通知
     *
     * page（默认1）：页码，从1开始
     *   - page=1 表示第一页
     *   - page=2 表示第二页
     *
     * size（默认10）：每页数量
     *   - size=10 每页返回10条公告
     *   - 手机端建议 size=10，平板端建议 size=20
     *
     * ═══════════════════════════════════════════════════════════════════
     * 排序规则
     * ═══════════════════════════════════════════════════════════════════
     *
     * 1. 置顶公告优先（isPinned=true 的排前面）
     * 2. 同级别内按发布时间降序（最新的排前面）
     *
     * ═══════════════════════════════════════════════════════════════════
     * 返回数据说明
     * ═══════════════════════════════════════════════════════════════════
     *
     * 返回 PageResult<BulletinListVO>，其中 BulletinListVO 不含正文内容，
     * 只包含标题、摘要、类型、时间等列表展示所需字段。
     * 这样做是为了减少网络传输量——列表页不需要加载每条公告的完整正文。
     *
     * @param type 公告类型（可选，值为 version/event/notice）
     * @param page 页码（从1开始，默认1）
     * @param size 每页数量（默认10）
     * @return Result<PageResult<BulletinListVO>> 分页公告列表
     */
    @Operation(summary = "获取公告列表", description = "分页查询公告列表，支持按类型筛选，置顶优先排序")
    @GetMapping
    public Result<PageResult<BulletinListVO>> getBulletinList(
            @Parameter(description = "公告类型（version/event/notice）") @RequestParam(required = false) String type,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        PageResult<BulletinListVO> result = bulletinService.getBulletinList(type, page, size);
        return Result.success(result);
    }

    /**
     * 获取最新公告 —— 用于首页轮播展示的轻量级接口
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：GET
     * 路径：/api/bulletins/latest
     * 权限：公开访问
     *
     * ═══════════════════════════════════════════════════════════════════
     * 使用场景
     * ═══════════════════════════════════════════════════════════════════
     *
     * 首页顶部有一个公告轮播组件，需要展示最新的几条公告标题。
     * 这个接口就是为轮播组件设计的，只返回最新的 N 条公告（默认3条）。
     *
     * 与 getBulletinList() 的区别：
     * - getBulletinList()：分页查询，支持类型筛选，返回 PageResult
     * - getLatestBulletins()：只返回最新N条，返回 List，更轻量
     *
     * @param limit 获取数量（默认3条，即轮播展示3条公告）
     * @return Result<List<BulletinListVO>> 最新公告列表（按发布时间降序）
     */
    @Operation(summary = "获取最新公告", description = "获取最新N条公告，用于首页轮播")
    @GetMapping("/latest")
    public Result<List<BulletinListVO>> getLatestBulletins(
            @Parameter(description = "获取数量") @RequestParam(defaultValue = "3") int limit) {
        List<BulletinListVO> result = bulletinService.getLatestBulletins(limit);
        return Result.success(result);
    }

    /**
     * 获取公告详情 —— 查看单条公告的完整内容
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：GET
     * 路径：/api/bulletins/{id}
     * 权限：公开访问
     *
     * ═══════════════════════════════════════════════════════════════════
     * 与列表接口的区别
     * ═══════════════════════════════════════════════════════════════════
     *
     * 列表接口（getBulletinList）返回 BulletinListVO，不含正文内容。
     * 详情接口（getBulletinDetail）返回 BulletinDetailVO，包含完整正文。
     *
     * 为什么要分开？
     * - 列表页只需要标题、摘要，不需要加载每条公告的完整正文
     * - 正文可能很长（几千字），如果列表接口也返回正文，会大大增加传输量
     * - 用户点击某条公告后，才通过详情接口加载完整内容
     *
     * @param id 公告ID（数据库主键）
     * @return Result<BulletinDetailVO> 公告详情（含完整正文）
     */
    @Operation(summary = "获取公告详情", description = "根据公告ID获取详细信息")
    @GetMapping("/{id}")
    public Result<BulletinDetailVO> getBulletinDetail(
            @Parameter(description = "公告ID") @PathVariable Long id) {
        BulletinDetailVO detail = bulletinService.getBulletinDetail(id);
        return Result.success(detail);
    }
}
