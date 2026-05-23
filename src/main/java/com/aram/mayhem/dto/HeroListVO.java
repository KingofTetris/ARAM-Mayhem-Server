package com.aram.mayhem.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 英雄列表视图对象（VO - View Object）
 *
 * VO 是专门给前端看的"展示数据"，和数据库实体（Entity）不同：
 * - Entity 包含数据库的所有字段（包括内部字段如 deleted）
 * - VO 只包含前端需要展示的字段，还可以加上计算字段
 *
 * 这个类用于英雄列表页面，展示每个英雄的卡片信息。
 * 就像商品列表页只显示商品图片、名称、价格，不显示库存编号等内部信息。
 *
 * 数据流向：
 * HeroService 从数据库查出 Hero 实体 → 手动映射为 HeroListVO → Controller 返回给前端
 *
 * 关联类：
 * - Hero：数据库实体类
 * - HeroService：负责 Entity → VO 的转换
 * - HeroController：将 VO 包装在 Result 中返回给前端
 */
@Data // Lombok 自动生成 getter/setter/toString/equals/hashCode
public class HeroListVO {

    /** 英雄 ID —— 唯一标识，前端点击时用这个 ID 跳转到详情页 */
    private Long id;

    /** 英雄英文名 —— 如 Aatrox，用于 URL 路径和数据匹配 */
    private String nameEn;

    /** 英雄中文名 —— 如 亚托克斯，前端列表页显示用 */
    private String nameZh;

    /** 英雄称号 —— 如 暗裔剑魔，显示在英雄名称下方 */
    private String title;

    /** 英雄定位 —— 如 战士、法师，用于筛选和分类 */
    private String role;

    /** 梯级评级 —— 如 S+/S/A/B/C，用颜色区分强度 */
    private BigDecimal winRate;

    /** ARAM 选取率 —— 如 0.1530 表示 15.30% */
    private BigDecimal pickRate;

    /** 英雄头像 URL —— 列表页显示英雄头像图片 */
    private String imageUrl;

    /** 是否为版本陷阱英雄 —— 前端用红色标记提醒用户 */
    private Boolean isVersionTrap;
}
