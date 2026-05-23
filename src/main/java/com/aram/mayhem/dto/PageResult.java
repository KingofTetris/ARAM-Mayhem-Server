package com.aram.mayhem.dto;

import lombok.Data;

import java.util.List;

/**
 * 通用分页结果包装类 —— 所有分页接口的"统一包装盒"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当后端返回分页数据时（如英雄列表、符文列表、公告列表），
 * 不能只返回一个列表，还需要告诉前端"总共有多少条数据"、"当前是第几页"等信息。
 * 这个类就是把这些分页信息打包在一起的"包装盒"。
 *
 * 就像快递包裹：
 * - records = 包裹里的商品（实际数据）
 * - total   = 仓库里这类商品的总库存（总记录数）
 * - page    = 你买的是第几箱（当前页码）
 * - size    = 每箱装了多少件（每页数量）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、为什么需要泛型 <T>？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 分页是通用需求，英雄列表要分页，符文列表也要分页，公告列表也要分页。
 * 如果不用泛型，就需要为每种数据类型写一个分页类：
 * - HeroPageResult（英雄分页）
 * - AugmentPageResult（符文分页）
 * - BulletinPageResult（公告分页）
 * 这三个类的结构完全一样，只是 records 的类型不同，太浪费了。
 *
 * 使用泛型 <T> 后，只需要这一个类就能满足所有分页需求：
 * - PageResult<HeroListVO>（英雄分页）
 * - PageResult<AugmentListVO>（符文分页）
 * - PageResult<BulletinListVO>（公告分页）
 * T 代表 records 列表中每个元素的具体类型，编译器会帮我们检查类型安全。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * 数据库（MySQL） → MyBatis-Plus 分页查询 → Service 层组装 PageResult
 * → Controller 层包装为 Result<PageResult<T>> → 前端
 *
 * 具体流程：
 * 1. Controller 接收分页请求（page=1, size=10）
 * 2. Service 调用 MyBatis-Plus 的 selectPage() 查询数据库
 * 3. 数据库返回当前页数据 + 总记录数
 * 4. Service 将数据组装为 PageResult<T> 对象
 * 5. Controller 包装为 Result<PageResult<T>> 返回给前端
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、前端如何使用分页数据？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 前端收到响应后，可以这样计算分页信息：
 * - 总页数 = Math.ceil(total / size)
 * - 是否有下一页 = page < 总页数
 * - 是否有上一页 = page > 1
 * - 当前页数据 = records
 *
 * 例如：total=95, page=1, size=10
 * - 总页数 = Math.ceil(95/10) = 10 页
 * - 第1页有10条记录，第10页只有5条记录
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、JSON 输出示例
 * ═══════════════════════════════════════════════════════════════════
 *
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": {
 *     "total": 165,        ← 数据库中共有165个英雄
 *     "page": 1,           ← 当前是第1页
 *     "size": 10,          ← 每页10个英雄
 *     "records": [         ← 第1页的10个英雄数据
 *       { "id": 1, "nameZh": "盖伦", "tier": "S+", ... },
 *       { "id": 2, "nameZh": "提莫", "tier": "A", ... },
 *       ... (共10条)
 *     ]
 *   },
 *   "timestamp": 1700000000000
 * }
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、使用此类的接口列表
 * ═══════════════════════════════════════════════════════════════════
 *
 * 接口                          | 泛型类型                    | 说明
 * ------------------------------|---------------------------|------------------
 * GET /api/heroes               | PageResult<HeroListVO>    | 英雄列表分页
 * GET /api/augments             | PageResult<AugmentListVO> | 符文列表分页
 * GET /api/bulletins            | PageResult<BulletinListVO>| 公告列表分页
 *
 * @param <T> 分页数据的元素类型（如 HeroListVO、AugmentListVO 等）
 *
 * 关联类：
 * @see com.aram.mayhem.common.Result 统一 API 响应包装类（PageResult 会被包在 Result 里返回）
 * @see com.aram.mayhem.dto.HeroListVO 英雄列表项
 * @see com.aram.mayhem.dto.AugmentListVO 符文列表项
 * @see com.aram.mayhem.dto.BulletinListVO 公告列表项
 */
@Data
public class PageResult<T> {

    /**
     * 总记录数 —— 数据库中符合条件的记录总数
     *
     * 用途：前端根据 total 和 size 计算总页数
     * 计算公式：总页数 = Math.ceil(total / size)
     *
     * 例如：total=165, size=10 → 总页数=17
     *
     * 注意：total 是"符合条件的总记录数"，不是"当前页的记录数"。
     * 即使当前页只有5条记录，total 仍然是165（表示数据库中共有165条匹配记录）。
     */
    private long total;

    /**
     * 当前页码 —— 从1开始计数
     *
     * page=1 表示第一页，page=2 表示第二页，以此类推。
     * 不从0开始，因为对普通用户来说"第1页"比"第0页"更容易理解。
     *
     * 前端分页组件通常这样使用：
     * - 上一页：page - 1（但不能小于1）
     * - 下一页：page + 1（但不能超过总页数）
     */
    private int page;

    /**
     * 每页数量 —— 每页显示的记录条数
     *
     * 常见取值：10、20、50
     * - 10：适合手机端（屏幕小，每页显示少一些）
     * - 20：适合平板端
     * - 50：适合桌面端（屏幕大，可以多显示一些）
     *
     * 前端可以通过下拉框让用户选择每页数量（如10/20/50），
     * 选择后重新请求接口，传入新的 size 值。
     */
    private int size;

    /**
     * 当前页数据列表 —— 本页的实际数据
     *
     * 列表长度通常等于 size，但最后一页可能少于 size。
     * 例如：total=25, size=10 → 第1页10条，第2页10条，第3页5条。
     *
     * 列表为空（records.size() == 0）的情况：
     * - page 超过了总页数（如总共3页，请求第5页）
     * - 筛选条件没有匹配到任何记录（如搜索"不存在英雄名"）
     */
    private List<T> records;

    /**
     * 构造分页结果 —— 创建包含完整分页信息的对象
     *
     * 使用示例：
     * // 查询到10个英雄，数据库共有165个英雄，当前第1页，每页10个
     * PageResult<HeroListVO> result = new PageResult<>(165, 1, 10, heroList);
     *
     * @param total   总记录数（数据库查询的 COUNT 结果）
     * @param page    当前页码（从1开始）
     * @param size    每页数量
     * @param records 当前页的数据列表（从数据库查询的当前页数据）
     */
    public PageResult() {
    }

    public PageResult(long total, int page, int size, List<T> records) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.records = records;
    }
}
