package com.aram.mayhem.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告详情视图对象 —— 公告详情页展示用
 *
 * 与 BulletinListVO 的区别：
 * - BulletinListVO：列表页，只有摘要
 * - BulletinDetailVO：详情页，包含完整的 HTML 正文
 *
 * 数据流向：
 * BulletinService 查询公告详情 → 映射为 BulletinDetailVO → 返回给前端
 *
 * 关联类：
 * - BulletinListVO：列表页视图对象
 * - Bulletin：数据库实体类
 */
@Data
public class BulletinDetailVO {

    /** 公告 ID */
    private Long id;

    /** 公告类型（version/event/notice） */
    private String type;

    /** 公告标题 */
    private String title;

    /** 公告完整正文 —— 支持 HTML 格式，前端用 WebView 渲染 */
    private String content;

    /** 封面图 URL */
    private String imageUrl;

    /** 是否置顶（1=置顶, 0=普通） */
    private Integer isPinned;

    /** 发布时间 */
    private LocalDateTime publishedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
